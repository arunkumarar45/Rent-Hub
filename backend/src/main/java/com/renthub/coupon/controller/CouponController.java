package com.renthub.coupon.controller;

import com.renthub.common.dto.ApiResponse;
import com.renthub.coupon.model.entity.Coupon;
import com.renthub.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Coupons & Discounts", description = "Coupon validation for booking checkouts and administrative coupon management.")
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/api/v1/coupons/validate")
    @Operation(summary = "Validate coupon code", description = "Validates a coupon code against a booking amount and returns its details if valid.")
    public ResponseEntity<ApiResponse<Coupon>> validateCoupon(
            @RequestParam String code,
            @RequestParam Integer bookingAmount) {
        Coupon coupon = couponService.validateAndGetCoupon(code, bookingAmount);
        return ResponseEntity.ok(ApiResponse.success(coupon, "Coupon is valid."));
    }

    @PostMapping("/api/v1/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create coupon", description = "Creates a new coupon code. Requires ADMIN role.")
    public ResponseEntity<ApiResponse<Coupon>> createCoupon(@Valid @RequestBody Coupon coupon) {
        Coupon created = couponService.createCoupon(coupon);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Coupon created successfully."));
    }

    @GetMapping("/api/v1/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List all coupons", description = "Lists all coupons in the system. Requires ADMIN role.")
    public ResponseEntity<ApiResponse<List<Coupon>>> getAllCoupons() {
        List<Coupon> coupons = couponService.getAllCoupons();
        return ResponseEntity.ok(ApiResponse.success(coupons, "Coupons retrieved successfully."));
    }

    @DeleteMapping("/api/v1/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete coupon", description = "Deletes an existing coupon code by its ID. Requires ADMIN role.")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Coupon deleted successfully."));
    }
}
