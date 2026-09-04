package com.bookmyshow.repository;

import com.bookmyshow.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);
    Optional<WalletTransaction> findByWalletIdAndReferenceTypeAndReferenceId(Long walletId, WalletReferenceType referenceType, String referenceId);
}
