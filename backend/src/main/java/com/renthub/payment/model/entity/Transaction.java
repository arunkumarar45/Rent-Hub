package com.renthub.payment.model.entity;

import com.renthub.auth.model.entity.User;
import com.renthub.booking.model.entity.Booking;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "payment_id", nullable = false, length = 100)
    private String paymentId; // Stripe Session ID or Razorpay Payment ID

    @Column(name = "order_id", length = 100)
    private String orderId; // Razorpay Order ID

    @Column(nullable = false)
    private Integer amount; // in cents

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @Column(nullable = false, length = 20)
    private String status; // PENDING, SUCCESS, FAILED, REFUNDED

    @Column(nullable = false, length = 20)
    private String method; // STRIPE or RAZORPAY

    @Column(nullable = false, length = 20)
    private String type; // PAYMENT or REFUND

    @Column(name = "refund_id", length = 100)
    private String refundId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
