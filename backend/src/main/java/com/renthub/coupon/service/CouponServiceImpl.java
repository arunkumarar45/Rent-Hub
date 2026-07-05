package com.renthub.coupon.service;

import com.renthub.common.exception.BadRequestException;
import com.renthub.common.exception.ResourceNotFoundException;
import com.renthub.coupon.model.entity.Coupon;
import com.renthub.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Override
    @Transactional(readOnly = true)
    public Coupon getCouponByCode(String code) {
        return couponRepository.findByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "code", code));
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon validateAndGetCoupon(String code, int bookingAmount) {
        Coupon coupon = getCouponByCode(code);
        
        if (!coupon.getIsActive()) {
            throw new BadRequestException("Coupon code is inactive.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartDate())) {
            throw new BadRequestException("Coupon promotion has not started yet.");
        }
        if (now.isAfter(coupon.getEndDate())) {
            throw new BadRequestException("Coupon promotion has expired.");
        }

        if (coupon.getMaxUses() > 0 && coupon.getUsesCount() >= coupon.getMaxUses()) {
            throw new BadRequestException("Coupon usage limit has been reached.");
        }

        if (bookingAmount < coupon.getMinBookingAmount()) {
            double minDollars = coupon.getMinBookingAmount() / 100.0;
            throw new BadRequestException(String.format("Minimum booking amount of $%.2f required to use this coupon.", minDollars));
        }

        return coupon;
    }

    @Override
    @Transactional
    public Coupon createCoupon(Coupon coupon) {
        coupon.setCode(coupon.getCode().toUpperCase().trim());
        if (couponRepository.findByCode(coupon.getCode()).isPresent()) {
            throw new BadRequestException("Coupon with code " + coupon.getCode() + " already exists.");
        }
        log.info("Created coupon code: {}", coupon.getCode());
        return couponRepository.save(coupon);
    }

    @Override
    @Transactional
    public void incrementUses(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", couponId));
        coupon.setUsesCount(coupon.getUsesCount() + 1);
        couponRepository.save(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        couponRepository.delete(coupon);
        log.info("Deleted coupon ID: {}", id);
    }
}
