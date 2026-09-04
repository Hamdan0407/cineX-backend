package com.bookmyshow.dto;

import java.math.BigDecimal;

public record WalletResponse(Long walletId, BigDecimal balance, String currency) {}
