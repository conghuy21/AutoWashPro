package com.autowashpro.controller;

import com.autowashpro.dto.request.FeedbackRequest;
import com.autowashpro.dto.response.FeedbackResponse;
import com.autowashpro.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /** POST /api/feedbacks — Khách gửi đánh giá */
    @PostMapping
    public ResponseEntity<FeedbackResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FeedbackRequest req) {
        return ResponseEntity.status(201)
                .body(feedbackService.createFeedback(
                        userDetails.getUsername(), req));
    }

    /** GET /api/feedbacks/booking/{bookingId} — Xem đánh giá theo booking */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<FeedbackResponse> getByBooking(
            @PathVariable Integer bookingId) {
        return ResponseEntity.ok(feedbackService.getByBookingId(bookingId));
    }

    /** GET /api/feedbacks/public — Xem đánh giá public */
    @GetMapping("/public")
    public ResponseEntity<List<FeedbackResponse>> getPublic() {
        return ResponseEntity.ok(feedbackService.getPublic());
    }

    /** GET /api/feedbacks — Staff/Manager xem tất cả */
    @GetMapping
    public ResponseEntity<List<FeedbackResponse>> getAll() {
        return ResponseEntity.ok(feedbackService.getAll());
    }

    /** PATCH /api/feedbacks/{id}/visibility — Admin ẩn/hiện */
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<FeedbackResponse> toggleVisibility(
            @PathVariable Integer id) {
        return ResponseEntity.ok(feedbackService.toggleVisibility(id));
    }
}