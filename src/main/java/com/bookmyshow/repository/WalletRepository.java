package com.bookmyshow.repository;

import com.bookmyshow.entity.Wallet;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByClerkUserId(String clerkUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.clerkUserId = :clerkUserId")
    Optional<Wallet> findByClerkUserIdForUpdate(@Param("clerkUserId") String clerkUserId);
}
