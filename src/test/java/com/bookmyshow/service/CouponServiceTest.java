package com.bookmyshow.service;

import com.bookmyshow.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CouponServiceTest {
    @Autowired private CouponService couponService;
    @Autowired private WalletService walletService;

    @Test
    void cinex300CreditsExactlyThreeHundredAndCreatesLedgerEntry() {
        var response = couponService.redeem("cinex300", "coupon-user-one");
        assertEquals("CINEX300", response.code());
        assertEquals(new BigDecimal("300.00"), response.creditedAmount());
        assertEquals(new BigDecimal("300.00"), response.wallet().balance());
        var transactions = walletService.getWalletTransactions("coupon-user-one");
        assertEquals(1, transactions.size());
        assertEquals("COUPON", transactions.get(0).referenceType());
        assertEquals("CINEX300 promotional wallet credit", transactions.get(0).description());
    }

    @Test
    void couponCannotBeRedeemedTwiceByTheSameUser() {
        couponService.redeem("CINEX300", "coupon-user-two");
        assertThrows(ValidationException.class, () -> couponService.redeem("CINEX300", "coupon-user-two"));
        assertEquals(new BigDecimal("300.00"), walletService.getWallet("coupon-user-two").balance());
        assertEquals(1, walletService.getWalletTransactions("coupon-user-two").size());
    }

    @Test
    void arbitraryCodesCannotCreateWalletCredit() {
        assertThrows(ValidationException.class, () -> couponService.redeem("CINEX999", "coupon-user-three"));
        assertEquals(new BigDecimal("0.00"), walletService.getWallet("coupon-user-three").balance());
    }
}
