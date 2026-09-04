package com.bookmyshow.repository;

import com.bookmyshow.entity.AbandonedCheckout;
import com.bookmyshow.entity.AbandonedCheckoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AbandonedCheckoutRepository extends JpaRepository<AbandonedCheckout, Long> {

    Optional<AbandonedCheckout> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    List<AbandonedCheckout> findByClerkUserIdAndStatusIn(String clerkUserId, Collection<AbandonedCheckoutStatus> statuses);

    /**
     * Part 2: incomplete checkouts ready for recovery email.
     * Failed/cancelled checkouts use eligibleForRecoveryAt; stale open checkouts use updatedAt.
     */
    @Query("""
            SELECT ac FROM AbandonedCheckout ac
            JOIN FETCH ac.booking b
            WHERE ac.recoveryEmailSent = false
            AND ac.status NOT IN (
                com.bookmyshow.entity.AbandonedCheckoutStatus.COMPLETED,
                com.bookmyshow.entity.AbandonedCheckoutStatus.RECOVERY_SENT
            )
            AND (
                (ac.status IN :failedOrCancelledStatuses
                    AND ac.eligibleForRecoveryAt IS NOT NULL
                    AND ac.eligibleForRecoveryAt <= :cutoff)
                OR
                (ac.status IN :staleOpenStatuses
                    AND ac.updatedAt <= :cutoff)
            )
            ORDER BY ac.updatedAt ASC
            """)
    List<AbandonedCheckout> findReadyForRecoveryProcessing(
            @Param("failedOrCancelledStatuses") Collection<AbandonedCheckoutStatus> failedOrCancelledStatuses,
            @Param("staleOpenStatuses") Collection<AbandonedCheckoutStatus> staleOpenStatuses,
            @Param("cutoff") LocalDateTime cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ac FROM AbandonedCheckout ac JOIN FETCH ac.booking b WHERE ac.id = :id")
    Optional<AbandonedCheckout> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE AbandonedCheckout ac
            SET ac.recoveryEmailSent = true,
                ac.recoveryEmailSentAt = :sentAt,
                ac.status = com.bookmyshow.entity.AbandonedCheckoutStatus.RECOVERY_SENT,
                ac.updatedAt = :sentAt
            WHERE ac.id = :id
            AND ac.recoveryEmailSent = false
            AND ac.status IN :allowedStatuses
            """)
    int markRecoverySentIfEligible(
            @Param("id") Long id,
            @Param("sentAt") LocalDateTime sentAt,
            @Param("allowedStatuses") Collection<AbandonedCheckoutStatus> allowedStatuses);
}
