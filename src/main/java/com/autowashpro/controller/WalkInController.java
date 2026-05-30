package com.autowashpro.controller;

import com.autowashpro.dto.request.WalkInRequest;
import com.autowashpro.dto.response.WalkInResponse;
import com.autowashpro.entity.ServiceLane;
import com.autowashpro.service.WalkInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/walkin")
@RequiredArgsConstructor
public class WalkInController {

    private final WalkInService walkInService;

    @PostMapping
    public ResponseEntity<WalkInResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WalkInRequest req) {
        return ResponseEntity.status(201)
                .body(walkInService.createWalkIn(
                        userDetails.getUsername(), req));
    }

    @GetMapping
    public ResponseEntity<List<WalkInResponse>> getAll() {
        return ResponseEntity.ok(walkInService.getAll());
    }

    @GetMapping("/today")
    public ResponseEntity<List<WalkInResponse>> getToday() {
        return ResponseEntity.ok(walkInService.getToday());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WalkInResponse> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                walkInService.updateStatus(
                        id, body.get("status"), body.get("paymentStatus")));
    }

    @GetMapping("/lanes")
    public ResponseEntity<List<ServiceLane>> getAvailableLanes() {
        return ResponseEntity.ok(walkInService.getAvailableLanes());
    }
}