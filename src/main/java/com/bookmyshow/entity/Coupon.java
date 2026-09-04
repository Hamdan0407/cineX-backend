package com.bookmyshow.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Data
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 64)
    private String code;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal walletCredit;
    @Column(nullable = false)
    private boolean active = true;
    private LocalDateTime expiresAt;
}
