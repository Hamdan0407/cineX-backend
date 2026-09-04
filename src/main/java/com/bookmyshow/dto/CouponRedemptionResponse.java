package com.bookmyshow.dto;

import java.math.BigDecimal;

public record CouponRedemptionResponse(String code, BigDecimal creditedAmount, WalletResponse wallet, String message) {}
