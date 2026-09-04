package com.bookmyshow.service;

import com.bookmyshow.entity.WalletReferenceType;
import com.bookmyshow.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class WalletServiceTest {
    @Autowired private WalletService walletService;

    @Test
    void newWalletStartsAtZeroAndHasNoTransactions() {
        var wallet = walletService.getOrCreateWallet("wallet-new-user");
        assertEquals(new BigDecimal("0.00"), wallet.balance());
        assertEquals("INR", wallet.currency());
        assertTrue(walletService.getWalletTransactions("wallet-new-user").isEmpty());
    }

    @Test
    void creditAndDebitCreateAuditableLedgerEntries() {
        walletService.creditWallet("wallet-ledger-user", new BigDecimal("300"), WalletReferenceType.PROMOTION, "promo-1", "Promotional credit");
        walletService.debitWallet("wallet-ledger-user", new BigDecimal("125"), WalletReferenceType.BOOKING, "booking-1", "Movie booking");

        assertEquals(new BigDecimal("175.00"), walletService.getWallet("wallet-ledger-user").balance());
        var entries = walletService.getWalletTransactions("wallet-ledger-user");
        assertEquals(2, entries.size());
        assertEquals("DEBIT", entries.get(0).type());
        assertEquals(new BigDecimal("300.00"), entries.get(1).balanceAfter());
    }

    @Test
    void duplicateBusinessReferenceIsIdempotent() {
        var first = walletService.creditWallet("wallet-idempotent-user", new BigDecimal("300"), WalletReferenceType.COUPON, "redemption-1", "Future coupon credit");
        var repeated = walletService.creditWallet("wallet-idempotent-user", new BigDecimal("300"), WalletReferenceType.COUPON, "redemption-1", "Future coupon credit");

        assertEquals(first.id(), repeated.id());
        assertEquals(new BigDecimal("300.00"), walletService.getWallet("wallet-idempotent-user").balance());
        assertEquals(1, walletService.getWalletTransactions("wallet-idempotent-user").size());
    }

    @Test
    void rejectsInvalidAmountsAndOverdrafts() {
        assertThrows(ValidationException.class, () -> walletService.creditWallet("wallet-safe-user", BigDecimal.ZERO, WalletReferenceType.PROMOTION, "zero", "Invalid"));
        walletService.getOrCreateWallet("wallet-safe-user");
        assertThrows(ValidationException.class, () -> walletService.debitWallet("wallet-safe-user", BigDecimal.ONE, WalletReferenceType.BOOKING, "booking-overdraft", "Invalid"));
        assertTrue(walletService.getWalletTransactions("wallet-safe-user").isEmpty());
    }
}
