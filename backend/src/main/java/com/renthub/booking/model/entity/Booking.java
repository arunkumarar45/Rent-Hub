package com.renthub.booking.model.entity;

import com.renthub.auth.model.entity.User;
import com.renthub.equipment.model.entity.Equipment;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "daily_rate", nullable = false)
    private Integer dailyRate; // in cents at booking time

    @Column(nullable = false)
    private Integer deposit; // in cents at booking time

    @Column(name = "rental_price", nullable = false)
    private Integer rentalPrice; // in cents (days * dailyRate, after coupon discounts)

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice; // in cents (rentalPrice + deposit)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @Column(name = "is_extension_requested", nullable = false)
    @Builder.Default
    private Boolean isExtensionRequested = false;

    @Column(name = "extension_end_date")
    private LocalDateTime extensionEndDate;

    @Column(name = "extension_price")
    private Integer extensionPrice; // in cents

    @Column(name = "extension_approved")
    private Boolean extensionApproved;

    @Column(name = "security_deposit_refunded", nullable = false)
    @Builder.Default
    private Boolean securityDepositRefunded = false;

    @Column(name = "penalty_amount", nullable = false)
    @Builder.Default
    private Integer penaltyAmount = 0; // in cents

    @Column(name = "invoice_url", length = 500)
    private String invoiceUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
