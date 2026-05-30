package com.autowashpro.service;

import com.autowashpro.dto.request.FeedbackRequest;
import com.autowashpro.dto.response.FeedbackResponse;
import com.autowashpro.entity.*;
import com.autowashpro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;

    /* ===== KHÁCH GỬI ĐÁNH GIÁ ===== */
    @Transactional
    public FeedbackResponse createFeedback(String email, FeedbackRequest req) {
        Customer customer = customerRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Booking booking = bookingRepository.findById(req.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Kiểm tra booking thuộc về customer
        if (!booking.getCustomer().getCustomerId()
                .equals(customer.getCustomerId())) {
            throw new RuntimeException("Booking không thuộc về bạn!");
        }

        // Kiểm tra booking đã hoàn tất chưa
        if (!"COMPLETED".equals(booking.getStatus())) {
            throw new RuntimeException("Chỉ có thể đánh giá booking đã hoàn tất!");
        }

        // Kiểm tra đã đánh giá chưa
        if (feedbackRepository.findByBooking_BookingId(req.getBookingId()).isPresent()) {
            throw new RuntimeException("Bạn đã đánh giá booking này rồi!");
        }

        Feedback feedback = new Feedback();
        feedback.setBooking(booking);
        feedback.setCustomer(customer);
        feedback.setRating(req.getRating());
        feedback.setComment(req.getComment());
        feedback.setIsVisible(true);

        return toResponse(feedbackRepository.save(feedback));
    }

    /* ===== XEM ĐÁNH GIÁ THEO BOOKING ===== */
    public FeedbackResponse getByBookingId(Integer bookingId) {
        return feedbackRepository.findByBooking_BookingId(bookingId)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Chưa có đánh giá cho booking này"));
    }

    /* ===== XEM TẤT CẢ (Staff/Manager) ===== */
    public List<FeedbackResponse> getAll() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /* ===== XEM PUBLIC (chỉ visible) ===== */
    public List<FeedbackResponse> getPublic() {
        return feedbackRepository.findByIsVisibleTrueOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /* ===== ẨN/HIỆN ĐÁNH GIÁ (Admin) ===== */
    @Transactional
    public FeedbackResponse toggleVisibility(Integer feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        feedback.setIsVisible(!feedback.getIsVisible());
        return toResponse(feedbackRepository.save(feedback));
    }

    private FeedbackResponse toResponse(Feedback f) {
        return FeedbackResponse.builder()
                .feedbackId(f.getFeedbackId())
                .bookingId(f.getBooking().getBookingId())
                .customerId(f.getCustomer().getCustomerId())
                .customerName(f.getCustomer().getUser().getFullName())
                .licensePlate(f.getBooking().getVehicle().getLicensePlate())
                .rating(f.getRating())
                .comment(f.getComment())
                .isVisible(f.getIsVisible())
                .createdAt(f.getCreatedAt())
                .build();
    }
}