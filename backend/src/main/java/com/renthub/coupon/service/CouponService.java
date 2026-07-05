package com.renthub.coupon.service;

import com.renthub.coupon.model.entity.Coupon;

import java.util.List;

public interface CouponService {
    Coupon getCouponByCode(String code);
    Coupon validateAndGetCoupon(String code, int bookingAmount);
    Coupon createCoupon(Coupon coupon);
    void incrementUses(Long couponId);
    List<Coupon> getAllCoupons();
    void deleteCoupon(Long id);
}
