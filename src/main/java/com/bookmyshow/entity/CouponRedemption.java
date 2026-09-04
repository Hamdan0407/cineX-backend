package com.bookmyshow.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_redemptions", uniqueConstraints = @UniqueConstraint(name = "uk_coupon_redemption_user_coupon", columnNames = {"clerk_user_id", "coupon_id"}))
@Data
public class CouponRedemption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "clerk_user_id", nullable = false, length = 128)
    private String clerkUserId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;
    @Column(nullable = false, length = 24)
    private String status = "REDEEMED";
    @Column(nullable = false, updatable = false)
    private LocalDateTime redeemedAt = LocalDateTime.now();
}
