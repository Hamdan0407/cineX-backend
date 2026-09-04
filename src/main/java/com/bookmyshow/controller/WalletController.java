package com.bookmyshow.controller;

import com.bookmyshow.dto.*;
import com.bookmyshow.service.AdminAuthService;
import com.bookmyshow.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;
    private final AdminAuthService adminAuthService;

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet() {
        return ResponseEntity.ok(walletService.getOrCreateWallet(adminAuthService.getAuthenticatedClerkUserId()));
    }
    @GetMapping("/transactions")
    public ResponseEntity<List<WalletTransactionResponse>> getTransactions() {
        return ResponseEntity.ok(walletService.getWalletTransactions(adminAuthService.getAuthenticatedClerkUserId()));
    }
}
