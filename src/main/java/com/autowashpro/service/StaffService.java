package com.autowashpro.service;

import com.autowashpro.dto.request.BookingStatusRequest;
import com.autowashpro.dto.response.BookingResponse;
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
public class StaffService {

    private final BookingRepository bookingRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    

    /* ===== XEM TẤT CẢ BOOKING HÔM NAY ===== */
    public List<BookingResponse> getTodayBookings(String email) {
        Employee employee = getEmployeeByEmail(email);
        java.time.LocalDate today = java.time.LocalDate.now();

        return bookingRepository.findAll().stream()
                .filter(b -> b.getBookingDate().equals(today))
                .map(b -> toResponse(b))
                .collect(Collectors.toList());
    }

    /* ===== XEM BOOKING ĐƯỢC PHÂN CÔNG ===== */
    public List<BookingResponse> getMyBookings(String email) {
        Employee employee = getEmployeeByEmail(email);

        return bookingRepository.findAll().stream()
                .filter(b -> b.getEmployee() != null &&
                        b.getEmployee().getEmployeeId()
                                .equals(employee.getEmployeeId()))
                .map(b -> toResponse(b))
                .collect(Collectors.toList());
    }

    /* ===== CẬP NHẬT TRẠNG THÁI BOOKING ===== */
    @Transactional
    public BookingResponse updateBookingStatus(
            String email, Integer bookingId, String status) {
        Employee employee = getEmployeeByEmail(email);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Validate trạng thái hợp lệ
        List<String> validStatuses = List.of(
                "CONFIRMED", "CHECKED_IN", "IN_PROGRESS", "COMPLETED", "NO_SHOW");
        if (!validStatuses.contains(status)) {
            throw new RuntimeException("Trạng thái không hợp lệ!");
        }

        booking.setStatus(status);
        booking.setEmployee(employee);

        // Cập nhật thời gian tương ứng
        switch (status) {
            case "CHECKED_IN" -> booking.setCheckinAt(LocalDateTime.now());
            case "IN_PROGRESS" -> booking.setStartAt(LocalDateTime.now());
            case "COMPLETED" -> booking.setCompleteAt(LocalDateTime.now());
        }

        return toResponse(bookingRepository.save(booking));
    }

    /* ===== PHÂN CÔNG NHÂN VIÊN CHO BOOKING ===== */
    @Transactional
    public BookingResponse assignBooking(String email, Integer bookingId) {
        Employee employee = getEmployeeByEmail(email);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setEmployee(employee);
        return toResponse(bookingRepository.save(booking));
    }

    // ===== HELPERS =====
    private Employee getEmployeeByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return employeeRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    private BookingResponse toResponse(Booking b) {
        BigDecimal subtotal = b.getBookingServices().stream()
                .map(bs -> bs.getPriceAtBooking())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return BookingResponse.builder()
                .bookingId(b.getBookingId())
                .bookingDate(b.getBookingDate())
                .status(b.getStatus())
                .bookingType(b.getBookingType())
                .qrCode(b.getQrCode())
                .notes(b.getNotes())
                .customerName(b.getCustomer().getUser().getFullName())
                .customerPhone(b.getCustomer().getUser().getPhone())
                .licensePlate(b.getVehicle().getLicensePlate())
                .vehicleType(b.getVehicle().getVehicleType().getName())
                .brand(b.getVehicle().getBrand())
                .model(b.getVehicle().getModel())
                .startTime(b.getTimeSlot().getStartTime())
                .endTime(b.getTimeSlot().getEndTime())
                .employeeName(b.getEmployee() != null
                        ? b.getEmployee().getUser().getFullName() : null)
                .subtotal(subtotal)
                .createdAt(b.getCreatedAt())
                .build();
    }
}