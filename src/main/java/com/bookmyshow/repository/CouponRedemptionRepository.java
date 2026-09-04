package com.bookmyshow.repository;

import com.bookmyshow.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
    Optional<CouponRedemption> findByClerkUserIdAndCouponId(String clerkUserId, Long couponId);
}
