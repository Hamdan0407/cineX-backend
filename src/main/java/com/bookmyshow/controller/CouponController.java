package com.bookmyshow.controller;

import com.bookmyshow.dto.*;
import com.bookmyshow.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bookmyshow.entity.Coupon;
import java.util.List;

import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;
    private final AdminAuthService adminAuthService;

    @PostMapping("/redeem")
    public ResponseEntity<CouponRedemptionResponse> redeem(@RequestBody CouponRedeemRequest request) {
        return ResponseEntity.ok(couponService.redeem(request.code(), adminAuthService.getAuthenticatedClerkUserId()));
    }

    @GetMapping
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        adminAuthService.validateAdmin();
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @PostMapping
    public ResponseEntity<Coupon> createCoupon(@RequestBody Map<String, Object> payload) {
        adminAuthService.validateAdmin();
        String code = payload.get("code") != null ? payload.get("code").toString() : "";
        BigDecimal credit = new BigDecimal("300.00");
        if (payload.get("walletCredit") != null) {
            try {
                credit = new BigDecimal(payload.get("walletCredit").toString());
            } catch (Exception ignored) {}
        }
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setWalletCredit(credit);
        coupon.setActive(true);
        return ResponseEntity.ok(couponService.createCoupon(coupon));
    }
}
