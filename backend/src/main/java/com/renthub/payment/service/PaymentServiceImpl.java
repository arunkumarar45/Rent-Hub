package com.renthub.payment.service;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.net.Webhook;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;

import com.renthub.auth.model.entity.User;
import com.renthub.auth.repository.UserRepository;
import com.renthub.booking.model.entity.Booking;
import com.renthub.booking.model.entity.BookingStatus;
import com.renthub.booking.repository.BookingRepository;
import com.renthub.common.exception.BadRequestException;
import com.renthub.common.exception.ResourceNotFoundException;
import com.renthub.common.util.SecurityUtils;
import com.renthub.payment.model.entity.Transaction;
import com.renthub.payment.repository.TransactionRepository;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final TransactionRepository transactionRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret:}")
    private String stripeWebhookSecret;

    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    @PostConstruct
    public void init() {
        if (stripeApiKey != null && !stripeApiKey.isEmpty()) {
            Stripe.apiKey = stripeApiKey;
            log.info("Stripe SDK initialized successfully.");
        } else {
            log.warn("Stripe API key is missing. Stripe payments will operate in MOCK mode.");
        }
    }

    @Override
    @Transactional
    public String createStripeCheckoutSession(Long bookingId, String idempotencyKey) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new BadRequestException("Duplicate request detected via idempotency key.");
        }

        // Mock mode fallback
        if (stripeApiKey == null || stripeApiKey.isEmpty()) {
            log.info("[MOCK STRIPE] Creating checkout session for booking: {}", bookingId);
            String mockSessionId = "cs_test_" + UUID.randomUUID();
            Transaction transaction = Transaction.builder()
                    .booking(booking)
                    .user(booking.getCustomer())
                    .paymentId(mockSessionId)
                    .amount(booking.getTotalPrice())
                    .status("PENDING")
                    .method("STRIPE")
                    .type("PAYMENT")
                    .idempotencyKey(idempotencyKey)
                    .build();
            transactionRepository.save(transaction);
            return "https://checkout.stripe.com/pay/" + mockSessionId;
        }

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:3000/dashboard/bookings?success=true")
                    .setCancelUrl("http://localhost:3000/dashboard/bookings?cancelled=true")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("usd")
                                                    .setUnitAmount((long) booking.getTotalPrice())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(booking.getEquipment().getTitle())
                                                                    .setDescription("Rental deposit + charges")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);
            
            Transaction transaction = Transaction.builder()
                    .booking(booking)
                    .user(booking.getCustomer())
                    .paymentId(session.getId())
                    .amount(booking.getTotalPrice())
                    .status("PENDING")
                    .method("STRIPE")
                    .type("PAYMENT")
                    .idempotencyKey(idempotencyKey)
                    .build();
            transactionRepository.save(transaction);

            return session.getUrl();
        } catch (Exception e) {
            log.error("Stripe session creation failed", e);
            throw new RuntimeException("Stripe error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String createRazorpayOrder(Long bookingId, String idempotencyKey) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new BadRequestException("Duplicate request detected.");
        }

        // Mock mode
        if (razorpayKeyId == null || razorpayKeyId.isEmpty()) {
            log.info("[MOCK RAZORPAY] Creating order for booking: {}", bookingId);
            String mockOrderId = "order_test_" + UUID.randomUUID().toString().substring(0, 14);
            Transaction transaction = Transaction.builder()
                    .booking(booking)
                    .user(booking.getCustomer())
                    .paymentId("pay_mock_" + UUID.randomUUID().toString().substring(0, 14))
                    .orderId(mockOrderId)
                    .amount(booking.getTotalPrice())
                    .status("PENDING")
                    .method("RAZORPAY")
                    .type("PAYMENT")
                    .idempotencyKey(idempotencyKey)
                    .build();
            transactionRepository.save(transaction);
            return mockOrderId;
        }

        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", booking.getTotalPrice()); // in paise (cents equivalent)
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + bookingId);

            Order order = client.orders.create(orderRequest);
            String orderId = order.get("id");

            Transaction transaction = Transaction.builder()
                    .booking(booking)
                    .user(booking.getCustomer())
                    .paymentId("pay_pending_" + UUID.randomUUID())
                    .orderId(orderId)
                    .amount(booking.getTotalPrice())
                    .status("PENDING")
                    .method("RAZORPAY")
                    .type("PAYMENT")
                    .idempotencyKey(idempotencyKey)
                    .build();
            transactionRepository.save(transaction);

            return orderId;
        } catch (Exception e) {
            log.error("Razorpay order creation failed", e);
            throw new RuntimeException("Razorpay error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void verifyStripePayment(String sessionId) {
        log.info("Verifying Stripe payment for session: {}", sessionId);
        // Find transaction
        Transaction transaction = transactionRepository.findAll().stream()
                .filter(t -> sessionId.equals(t.getPaymentId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "paymentId", sessionId));

        // Mock verification
        if (stripeApiKey == null || stripeApiKey.isEmpty()) {
            transaction.setStatus("SUCCESS");
            transactionRepository.save(transaction);
            
            // Set booking status to ACTIVE
            Booking booking = transaction.getBooking();
            booking.setStatus(BookingStatus.ACTIVE);
            bookingRepository.save(booking);
            
            log.info("Mock verification success for Stripe transaction");
            return;
        }

        try {
            Session session = Session.retrieve(sessionId);
            if ("paid".equals(session.getPaymentStatus())) {
                transaction.setStatus("SUCCESS");
                transactionRepository.save(transaction);
                
                // Set booking status to ACTIVE
                Booking booking = transaction.getBooking();
                booking.setStatus(BookingStatus.ACTIVE);
                bookingRepository.save(booking);
                
                log.info("Stripe transaction marked as SUCCESS");
            } else {
                transaction.setStatus("FAILED");
                transactionRepository.save(transaction);
            }
        } catch (Exception e) {
            log.error("Stripe verification failed", e);
        }
    }

    @Override
    @Transactional
    public void verifyRazorpayPayment(String paymentId, String orderId, String signature) {
        log.info("Verifying Razorpay payment ID: {}", paymentId);
        Transaction transaction = transactionRepository.findAll().stream()
                .filter(t -> orderId.equals(t.getOrderId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "orderId", orderId));

        if (razorpayKeyId == null || razorpayKeyId.isEmpty()) {
            transaction.setPaymentId(paymentId);
            transaction.setStatus("SUCCESS");
            transactionRepository.save(transaction);
            
            // Set booking status to ACTIVE
            Booking booking = transaction.getBooking();
            booking.setStatus(BookingStatus.ACTIVE);
            bookingRepository.save(booking);
            return;
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isValid = com.razorpay.Utils.verifyPaymentSignature(options, razorpayKeySecret);
            if (isValid) {
                transaction.setPaymentId(paymentId);
                transaction.setStatus("SUCCESS");
                transactionRepository.save(transaction);
                
                // Set booking status to ACTIVE
                Booking booking = transaction.getBooking();
                booking.setStatus(BookingStatus.ACTIVE);
                bookingRepository.save(booking);
                
                log.info("Razorpay transaction marked as SUCCESS");
            } else {
                transaction.setStatus("FAILED");
                transactionRepository.save(transaction);
            }
        } catch (Exception e) {
            log.error("Razorpay signature validation failed", e);
            throw new BadRequestException("Invalid payment signature");
        }
    }

    @Override
    @Transactional
    public void handleStripeWebhook(String payload, String sigHeader) {
        if (stripeWebhookSecret == null || stripeWebhookSecret.isEmpty()) {
            log.warn("Stripe webhook endpoint secret is missing. Processing skipped.");
            return;
        }
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            if (dataObjectDeserializer.getObject().isPresent()) {
                Session session = (Session) dataObjectDeserializer.getObject().get();
                if ("checkout.session.completed".equals(event.getType())) {
                    verifyStripePayment(session.getId());
                }
            }
        } catch (Exception e) {
            log.error("Stripe Webhook signature verification failed", e);
        }
    }

    @Override
    @Transactional
    public void handleRazorpayWebhook(String payload, String signature) {
        log.info("Received Razorpay webhook");
        // Simple log since webhook signature verification requires verifying against endpoint secrets.
    }

    @Override
    @Transactional
    public void refundPayment(Long bookingId) {
        log.info("Processing refund request for booking ID: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        List<Transaction> txns = transactionRepository.findByBookingId(bookingId).stream()
                .filter(t -> "SUCCESS".equals(t.getStatus()) && "PAYMENT".equals(t.getType()))
                .collect(Collectors.toList());

        if (txns.isEmpty()) {
            log.warn("No successful payments found to refund for booking ID: {}", bookingId);
            return;
        }

        Transaction origTxn = txns.get(0);

        // Mock refund
        if (stripeApiKey == null || stripeApiKey.isEmpty()) {
            Transaction refundTxn = Transaction.builder()
                    .booking(booking)
                    .user(booking.getCustomer())
                    .paymentId("ref_mock_" + UUID.randomUUID())
                    .amount(origTxn.getAmount())
                    .status("SUCCESS")
                    .method(origTxn.getMethod())
                    .type("REFUND")
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build();
            transactionRepository.save(refundTxn);
            origTxn.setStatus("REFUNDED");
            transactionRepository.save(origTxn);
            log.info("Mock refund successful for booking ID: {}", bookingId);
            return;
        }

        try {
            if ("STRIPE".equals(origTxn.getMethod())) {
                RefundCreateParams params = RefundCreateParams.builder()
                        .setPaymentIntent(origTxn.getPaymentId()) // or session charge ID depending on setup
                        .build();
                Refund refund = Refund.create(params);

                Transaction refundTxn = Transaction.builder()
                        .booking(booking)
                        .user(booking.getCustomer())
                        .paymentId(refund.getId())
                        .amount(origTxn.getAmount())
                        .status("SUCCESS")
                        .method("STRIPE")
                        .type("REFUND")
                        .idempotencyKey(UUID.randomUUID().toString())
                        .build();
                transactionRepository.save(refundTxn);
                origTxn.setStatus("REFUNDED");
                transactionRepository.save(origTxn);
            }
        } catch (Exception e) {
            log.error("Failed to execute refund via gateway API", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return transactionRepository.findByUserId(user.getId());
    }
}
