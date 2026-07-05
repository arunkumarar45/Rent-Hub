package com.renthub.payment.controller;

import com.renthub.common.dto.ApiResponse;
import com.renthub.payment.model.entity.Transaction;
import com.renthub.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments & Transactions", description = "Endpoints for initiating Stripe Checkout or Razorpay orders, verifications, and transaction logs.")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/stripe/checkout")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create Stripe Checkout session", description = "Initializes a Stripe Checkout session and logs a pending transaction.")
    public ResponseEntity<ApiResponse<String>> createStripeCheckout(
            @RequestParam Long bookingId,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        String url = paymentService.createStripeCheckoutSession(bookingId, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(url, "Stripe Checkout URL generated successfully."));
    }

    @PostMapping("/razorpay/order")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create Razorpay Order", description = "Initializes a Razorpay order in the gateway and logs a pending transaction.")
    public ResponseEntity<ApiResponse<String>> createRazorpayOrder(
            @RequestParam Long bookingId,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        String orderId = paymentService.createRazorpayOrder(bookingId, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(orderId, "Razorpay Order ID generated successfully."));
    }

    @PostMapping("/stripe/verify")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Verify Stripe Payment", description = "Verifies Stripe session status and marks transaction as SUCCESS.")
    public ResponseEntity<ApiResponse<Void>> verifyStripe(@RequestParam String sessionId) {
        paymentService.verifyStripePayment(sessionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Stripe payment verified successfully."));
    }

    @PostMapping("/razorpay/verify")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Verify Razorpay Payment", description = "Validates the Razorpay HMAC signature and marks transaction as SUCCESS.")
    public ResponseEntity<ApiResponse<Void>> verifyRazorpay(
            @RequestParam String paymentId,
            @RequestParam String orderId,
            @RequestParam String signature) {
        paymentService.verifyRazorpayPayment(paymentId, orderId, signature);
        return ResponseEntity.ok(ApiResponse.success(null, "Razorpay signature validated successfully."));
    }

    @PostMapping("/webhook/stripe")
    @Operation(summary = "Stripe webhook receiver", description = "Listens to events from Stripe like checkout.session.completed.")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.handleStripeWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhook/razorpay")
    @Operation(summary = "Razorpay webhook receiver", description = "Listens to events from Razorpay.")
    public ResponseEntity<Void> razorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        paymentService.handleRazorpayWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get transaction history", description = "Retrieves all payment/refund transaction entries for the current user.")
    public ResponseEntity<ApiResponse<List<Transaction>>> getTransactionHistory() {
        List<Transaction> history = paymentService.getTransactionHistory();
        return ResponseEntity.ok(ApiResponse.success(history, "Transaction history retrieved."));
    }
}
