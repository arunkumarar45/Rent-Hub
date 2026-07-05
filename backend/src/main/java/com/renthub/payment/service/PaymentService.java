package com.renthub.payment.service;

import com.renthub.payment.model.entity.Transaction;

import java.util.List;

public interface PaymentService {
    String createStripeCheckoutSession(Long bookingId, String idempotencyKey);
    String createRazorpayOrder(Long bookingId, String idempotencyKey);
    void verifyStripePayment(String sessionId);
    void verifyRazorpayPayment(String paymentId, String orderId, String signature);
    void handleStripeWebhook(String payload, String sigHeader);
    void handleRazorpayWebhook(String payload, String signature);
    void refundPayment(Long bookingId);
    List<Transaction> getTransactionHistory();
}
