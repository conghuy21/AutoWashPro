package com.autowashpro.controller;

import com.autowashpro.dto.response.BookingResponse;
import com.autowashpro.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    /** GET /api/staff/bookings/today — Booking hôm nay */
    @GetMapping("/bookings/today")
    public ResponseEntity<List<BookingResponse>> getTodayBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                staffService.getTodayBookings(userDetails.getUsername()));
    }

    /** GET /api/staff/bookings/my — Booking được phân công */
    @GetMapping("/bookings/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                staffService.getMyBookings(userDetails.getUsername()));
    }

    /** PATCH /api/staff/bookings/{id}/status — Cập nhật trạng thái */
    @PatchMapping("/bookings/{id}/status")
    public ResponseEntity<BookingResponse> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                staffService.updateBookingStatus(
                        userDetails.getUsername(), id, body.get("status")));
    }

    /** PATCH /api/staff/bookings/{id}/assign — Tự nhận booking */
    @PatchMapping("/bookings/{id}/assign")
    public ResponseEntity<BookingResponse> assignBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                staffService.assignBooking(userDetails.getUsername(), id));
    }
}