package com.bookmyshow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletTransactionResponse(Long id, String type, BigDecimal amount, BigDecimal balanceBefore,
                                        BigDecimal balanceAfter, String referenceType, String referenceId,
                                        String description, LocalDateTime createdAt) {}
