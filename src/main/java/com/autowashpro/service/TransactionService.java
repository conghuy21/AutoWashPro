package com.autowashpro.service;

import com.autowashpro.dto.request.TransactionRequest;
import com.autowashpro.dto.response.TransactionResponse;
import com.autowashpro.entity.*;
import com.autowashpro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final PromotionRepository promotionRepository;
    private final PayOSService payOSService;
    private final LoyaltyService loyaltyService;

    @Transactional
    public TransactionResponse createTransaction(String email, TransactionRequest req) {

        Customer customer = customerRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Booking booking = bookingRepository.findById(req.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new RuntimeException("Access denied");
        }

        if (!List.of("PENDING", "CONFIRMED").contains(booking.getStatus())) {
            throw new RuntimeException("Booking không thể thanh toán ở trạng thái: "
                    + booking.getStatus());
        }

        // Kiểm tra transaction cũ
var existingTransaction = transactionRepository
        .findByBooking_BookingId(booking.getBookingId());

if (existingTransaction.isPresent()) {
    Transaction existing = existingTransaction.get();
    if ("PAID".equals(existing.getPaymentStatus())) {
        throw new RuntimeException("Booking này đã thanh toán rồi!");
    }
    // PENDING hoặc CANCELLED → xóa đi để tạo mới
    transactionRepository.delete(existing);
    // Ép database xóa luôn lập tức!
    transactionRepository.flush();
}

        // Tính subtotal
        BigDecimal subtotal = booking.getBookingServices().stream()
                .map(BookingServiceItem::getPriceAtBooking)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Áp dụng promotion
        BigDecimal discountAmount = BigDecimal.ZERO;
        Promotion promotion = null;

        if (req.getPromotionCode() != null && !req.getPromotionCode().isEmpty()) {
            promotion = promotionRepository
                    .findByCodeAndIsActiveTrue(req.getPromotionCode())
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ"));

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(promotion.getStartAt()) || now.isAfter(promotion.getEndAt())) {
                throw new RuntimeException("Mã giảm giá đã hết hạn");
            }
            if (subtotal.compareTo(promotion.getMinOrderValue()) < 0) {
                throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu");
            }
            if (promotion.getUsageLimit() != null
                    && promotion.getUsedCount() >= promotion.getUsageLimit()) {
                throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng");
            }

            if ("PERCENT".equals(promotion.getDiscountType())) {
                discountAmount = subtotal.multiply(promotion.getDiscountValue())
                        .divide(BigDecimal.valueOf(100));
                if (promotion.getMaxDiscount() != null
                        && discountAmount.compareTo(promotion.getMaxDiscount()) > 0) {
                    discountAmount = promotion.getMaxDiscount();
                }
            } else {
                discountAmount = promotion.getDiscountValue();
                if (discountAmount.compareTo(subtotal) > 0) discountAmount = subtotal;
            }

            promotion.setUsedCount(promotion.getUsedCount() + 1);
            promotionRepository.save(promotion);
        }

        BigDecimal finalAmount = subtotal.subtract(discountAmount);

        Transaction transaction = new Transaction();
        transaction.setBooking(booking);
        transaction.setCustomer(customer);
        transaction.setSubtotal(subtotal);
        transaction.setDiscountAmount(discountAmount);
        transaction.setFinalAmount(finalAmount);
        transaction.setPaymentMethod(req.getPaymentMethod());
        transaction.setPromotion(promotion);
        transaction.setPointsUsed(0);

        String checkoutUrl = null;

        if ("CASH".equals(req.getPaymentMethod())) {
            transaction.setPaymentStatus("PAID");
            transaction.setPaidAt(LocalDateTime.now());
            booking.setStatus("COMPLETED");
            bookingRepository.save(booking);
            transactionRepository.save(transaction);
            // Tự động cộng điểm sau thanh toán 
    loyaltyService.addPointsAfterPayment(customer, finalAmount.intValue());

        } else if ("BANK_TRANSFER".equals(req.getPaymentMethod())) {
            transaction.setPaymentStatus("PENDING");
            transactionRepository.save(transaction);

        } else if ("PAYOS".equals(req.getPaymentMethod())) {
            transaction.setPaymentStatus("PENDING");
            transactionRepository.save(transaction);
            try {
                checkoutUrl = payOSService.createPaymentLink(transaction);
            } catch (Exception e) {
                throw new RuntimeException("Tạo PayOS link thất bại: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("Payment method không hợp lệ");
        }

        TransactionResponse response = toResponse(transaction, promotion);
        response.setCheckoutUrl(checkoutUrl);
        return response;
    }

    /* ===== XÁC NHẬN THANH TOÁN (Staff) ===== */
    @Transactional
    public TransactionResponse confirmPayment(Integer transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!"PENDING".equals(transaction.getPaymentStatus())) {
            throw new RuntimeException("Transaction không ở trạng thái PENDING");
        }

        transaction.setPaymentStatus("PAID");
        transaction.setPaidAt(LocalDateTime.now());

        Booking booking = transaction.getBooking();
        booking.setStatus("COMPLETED");
        bookingRepository.save(booking);
        transactionRepository.save(transaction);

        return toResponse(transaction, transaction.getPromotion());
    }

    /* ===== XEM THEO BOOKING ===== */
    public TransactionResponse getByBookingId(Integer bookingId) {
        Transaction t = transactionRepository.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        return toResponse(t, t.getPromotion());
    }

    /* ===== LỊCH SỬ THANH TOÁN ===== */
    public List<TransactionResponse> getMyTransactions(String email) {
        Customer customer = customerRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return transactionRepository
                .findByCustomer_CustomerIdOrderByCreatedAtDesc(customer.getCustomerId())
                .stream()
                .map(t -> toResponse(t, t.getPromotion()))
                .collect(Collectors.toList());
    }

    private TransactionResponse toResponse(Transaction t, Promotion promotion) {
        return TransactionResponse.builder()
                .transactionId(t.getTransactionId())
                .bookingId(t.getBooking().getBookingId())
                .customerName(t.getCustomer().getUser().getFullName())
                .subtotal(t.getSubtotal())
                .discountAmount(t.getDiscountAmount())
                .finalAmount(t.getFinalAmount())
                .paymentMethod(t.getPaymentMethod())
                .paymentStatus(t.getPaymentStatus())
                .promotionCode(promotion != null ? promotion.getCode() : null)
                .pointsUsed(t.getPointsUsed())
                .paidAt(t.getPaidAt())
                .createdAt(t.getCreatedAt())
                .build();
    }
}