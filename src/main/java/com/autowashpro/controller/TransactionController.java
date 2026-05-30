package com.autowashpro.controller;

import com.autowashpro.dto.request.TransactionRequest;
import com.autowashpro.dto.response.TransactionResponse;
import com.autowashpro.service.PayOSService;
import com.autowashpro.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final PayOSService payOSService;

    /** POST /api/transactions — Customer tạo transaction */
    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.status(201).body(
                transactionService.createTransaction(userDetails.getUsername(), req));
    }

    /** GET /api/transactions/my — Lịch sử thanh toán */
    @GetMapping("/my")
    public ResponseEntity<List<TransactionResponse>> getMyTransactions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                transactionService.getMyTransactions(userDetails.getUsername()));
    }

    /** GET /api/transactions/booking/{bookingId} */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<TransactionResponse> getByBooking(
            @PathVariable Integer bookingId) {
        return ResponseEntity.ok(
                transactionService.getByBookingId(bookingId));
    }

    /** PATCH /api/transactions/{id}/confirm — Staff xác nhận */
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<TransactionResponse> confirm(@PathVariable Integer id) {
        return ResponseEntity.ok(transactionService.confirmPayment(id));
    }

   /** POST /api/transactions/payos/webhook — PayOS callback */
@PostMapping("/payos/webhook")
public ResponseEntity<String> payosWebhook(@RequestBody String body) {
    try {
        // Nếu body rỗng hoặc là test request → trả OK luôn
        if (body == null || body.isEmpty() || body.equals("{}")) {
            return ResponseEntity.ok("OK");
        }
        payOSService.handleWebhook(body);
        return ResponseEntity.ok("OK");
    } catch (Exception e) {
        // Trả OK để PayOS không retry liên tục
        return ResponseEntity.ok("OK");
    }
}

    /** PATCH /api/transactions/{id}/cancel-payos — Hủy PayOS */
    @PatchMapping("/{id}/cancel-payos")
    public ResponseEntity<String> cancelPayos(@PathVariable Integer id) {
        try {
            payOSService.cancelPaymentLink(id);
            return ResponseEntity.ok("Cancelled");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}