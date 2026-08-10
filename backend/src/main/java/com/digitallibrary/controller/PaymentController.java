package com.digitallibrary.controller;

import com.digitallibrary.dto.ApiResponse;
import com.digitallibrary.dto.CheckoutRequest;
import com.digitallibrary.dto.PageResponse;
import com.digitallibrary.dto.PaymentResponse;
import com.digitallibrary.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request,
            Principal principal) {
        PaymentResponse payment = paymentService.initiateCheckout(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Checkout initiated successfully", payment));
    }

    @PostMapping("/webhook/{gateway}")
    public ResponseEntity<ApiResponse<PaymentResponse>> handleWebhook(
            @PathVariable String gateway,
            @RequestBody String payload,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        PaymentResponse payment = paymentService.handleWebhook(gateway, payload, signature);
        return ResponseEntity.ok(ApiResponse.success("Webhook processed", payment));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        PageResponse<PaymentResponse> payments = paymentService.getUserPayments(principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Payment history retrieved", payments));
    }
}
