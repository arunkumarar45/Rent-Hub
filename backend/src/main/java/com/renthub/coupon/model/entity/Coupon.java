package com.renthub.coupon.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType; // "PERCENTAGE" or "FIXED_AMOUNT"

    @Column(nullable = false)
    private Integer value; // in cents or percentage points

    @Column(name = "min_booking_amount", nullable = false)
    @Builder.Default
    private Integer minBookingAmount = 0; // in cents

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "max_uses", nullable = false)
    @Builder.Default
    private Integer maxUses = 0; // 0 means unlimited

    @Column(name = "uses_count", nullable = false)
    @Builder.Default
    private Integer usesCount = 0;
}
