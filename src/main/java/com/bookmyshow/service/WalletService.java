package com.bookmyshow.service;

import com.bookmyshow.dto.*;
import com.bookmyshow.entity.*;
import com.bookmyshow.exception.ValidationException;
import com.bookmyshow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @Transactional
    public WalletResponse getOrCreateWallet(String clerkUserId) {
        Wallet wallet = walletRepository.findByClerkUserId(clerkUserId).orElseGet(() -> {
            Wallet created = new Wallet();
            created.setClerkUserId(requireUser(clerkUserId));
            created.setBalance(BigDecimal.ZERO.setScale(2));
            return walletRepository.save(created);
        });
        return toResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(String clerkUserId) {
        return walletRepository.findByClerkUserId(requireUser(clerkUserId))
                .map(this::toResponse)
                .orElse(new WalletResponse(null, BigDecimal.ZERO.setScale(2), "INR"));
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getWalletTransactions(String clerkUserId) {
        return walletRepository.findByClerkUserId(requireUser(clerkUserId))
                .map(wallet -> transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()).stream().map(this::toTransactionResponse).toList())
                .orElse(List.of());
    }

    /** Business-only API for future coupon, refund, and promotion flows. */
    @Transactional
    public WalletTransactionResponse creditWallet(String clerkUserId, BigDecimal amount, WalletReferenceType referenceType,
                                                   String referenceId, String description) {
        return changeBalance(clerkUserId, amount, WalletTransactionType.CREDIT, referenceType, referenceId, description);
    }

    /** Business-only API for future wallet payment flows. */
    @Transactional
    public WalletTransactionResponse debitWallet(String clerkUserId, BigDecimal amount, WalletReferenceType referenceType,
                                                  String referenceId, String description) {
        return changeBalance(clerkUserId, amount, WalletTransactionType.DEBIT, referenceType, referenceId, description);
    }

    private WalletTransactionResponse changeBalance(String clerkUserId, BigDecimal amount, WalletTransactionType type,
                                                    WalletReferenceType referenceType, String referenceId, String description) {
        BigDecimal safeAmount = validateAmount(amount);
        if (referenceType == null || referenceId == null || referenceId.isBlank()) {
            throw new ValidationException("A wallet transaction reference is required");
        }
        Wallet wallet = walletRepository.findByClerkUserIdForUpdate(requireUser(clerkUserId))
                .orElseGet(() -> createWallet(clerkUserId));

        var prior = transactionRepository.findByWalletIdAndReferenceTypeAndReferenceId(wallet.getId(), referenceType, referenceId);
        if (prior.isPresent()) {
            WalletTransaction transaction = prior.get();
            if (transaction.getTransactionType() != type || transaction.getAmount().compareTo(safeAmount) != 0) {
                throw new ValidationException("Wallet reference was already used with a different transaction");
            }
            return toTransactionResponse(transaction);
        }

        BigDecimal before = wallet.getBalance();
        BigDecimal after = type == WalletTransactionType.CREDIT ? before.add(safeAmount) : before.subtract(safeAmount);
        if (after.signum() < 0) {
            throw new ValidationException("Insufficient wallet balance");
        }
        wallet.setBalance(after);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setTransactionType(type);
        transaction.setAmount(safeAmount);
        transaction.setBalanceBefore(before);
        transaction.setBalanceAfter(after);
        transaction.setReferenceType(referenceType);
        transaction.setReferenceId(referenceId.trim());
        transaction.setDescription(description == null || description.isBlank() ? "CineX wallet transaction" : description.trim());
        return toTransactionResponse(transactionRepository.save(transaction));
    }

    private Wallet createWallet(String clerkUserId) {
        Wallet wallet = new Wallet();
        wallet.setClerkUserId(requireUser(clerkUserId));
        wallet.setBalance(BigDecimal.ZERO.setScale(2));
        return walletRepository.save(wallet);
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new ValidationException("Wallet amount must be positive");
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
    private String requireUser(String clerkUserId) {
        if (clerkUserId == null || clerkUserId.isBlank()) throw new ValidationException("Authenticated user is required");
        return clerkUserId;
    }
    private WalletResponse toResponse(Wallet wallet) { return new WalletResponse(wallet.getId(), wallet.getBalance(), wallet.getCurrency()); }
    private WalletTransactionResponse toTransactionResponse(WalletTransaction t) {
        return new WalletTransactionResponse(t.getId(), t.getTransactionType().name(), t.getAmount(), t.getBalanceBefore(), t.getBalanceAfter(),
                t.getReferenceType().name(), t.getReferenceId(), t.getDescription(), t.getCreatedAt());
    }
}
