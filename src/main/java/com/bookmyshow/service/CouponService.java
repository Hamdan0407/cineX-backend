package com.bookmyshow.service;

import com.bookmyshow.dto.*;
import com.bookmyshow.entity.*;
import com.bookmyshow.exception.ValidationException;
import com.bookmyshow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponService {
    private static final String CINEX300 = "CINEX300";
    private static final BigDecimal CINEX300_CREDIT = new BigDecimal("300.00");
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final WalletService walletService;

    @Transactional
    public CouponRedemptionResponse redeem(String code, String clerkUserId) {
        if (code == null || !CINEX300.equalsIgnoreCase(code.trim())) throw new ValidationException("Coupon code is invalid");
        Coupon coupon = getOrSeedCinex300();
        // Serialize redemptions of this promotion so the database uniqueness rule remains a clean user error.
        coupon = couponRepository.findByCodeForUpdate(CINEX300).orElseThrow(() -> new ValidationException("Coupon is unavailable"));
        if (!coupon.isActive() || (coupon.getExpiresAt() != null && !coupon.getExpiresAt().isAfter(LocalDateTime.now()))) {
            throw new ValidationException("Coupon has expired or is unavailable");
        }
        if (redemptionRepository.findByClerkUserIdAndCouponId(clerkUserId, coupon.getId()).isPresent()) {
            throw new ValidationException("CINEX300 has already been redeemed");
        }
        CouponRedemption redemption = new CouponRedemption();
        redemption.setClerkUserId(clerkUserId);
        redemption.setCoupon(coupon);
        redemption = redemptionRepository.saveAndFlush(redemption);
        walletService.creditWallet(clerkUserId, coupon.getWalletCredit(), WalletReferenceType.COUPON,
                "coupon-redemption-" + redemption.getId(), "CINEX300 promotional wallet credit");
        return new CouponRedemptionResponse(coupon.getCode(), coupon.getWalletCredit(), walletService.getWallet(clerkUserId), "₹300 added to your CineX Wallet.");
    }

    public java.util.List<Coupon> getAllCoupons() {
        getOrSeedCinex300();
        return couponRepository.findAll();
    }

    @Transactional
    public Coupon createCoupon(Coupon coupon) {
        if (coupon.getCode() == null || coupon.getCode().trim().isEmpty()) {
            throw new ValidationException("Coupon code is required");
        }
        String code = coupon.getCode().trim().toUpperCase();
        if (couponRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new ValidationException("Coupon code already exists");
        }
        coupon.setCode(code);
        if (coupon.getWalletCredit() == null || coupon.getWalletCredit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Wallet credit amount must be greater than zero");
        }
        coupon.setActive(true);
        return couponRepository.save(coupon);
    }

    private Coupon getOrSeedCinex300() {
        return couponRepository.findByCodeIgnoreCase(CINEX300).orElseGet(() -> {
            Coupon coupon = new Coupon(); coupon.setCode(CINEX300); coupon.setWalletCredit(CINEX300_CREDIT); coupon.setActive(true);
            return couponRepository.save(coupon);
        });
    }
}
