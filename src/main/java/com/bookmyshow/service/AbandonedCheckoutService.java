package com.bookmyshow.service;

import com.bookmyshow.entity.AbandonedCheckout;
import com.bookmyshow.entity.AbandonedCheckoutStatus;
import com.bookmyshow.entity.Booking;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.repository.AbandonedCheckoutRepository;
import com.bookmyshow.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Tracks incomplete checkouts for abandoned-cart recovery (email in Part 2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbandonedCheckoutService {

    private static final Set<AbandonedCheckoutStatus> TERMINAL = EnumSet.of(
            AbandonedCheckoutStatus.COMPLETED,
            AbandonedCheckoutStatus.RECOVERY_SENT
    );

    private final AbandonedCheckoutRepository abandonedCheckoutRepository;
    private final BookingRepository bookingRepository;

    /**
     * Creates or returns the existing abandoned-checkout row for this booking (idempotent).
     */
    @Transactional
    public AbandonedCheckout trackCheckoutStarted(Booking booking) {
        if (booking == null || booking.getId() == null) {
            throw new IllegalArgumentException("Booking with id is required to track checkout");
        }

        Optional<AbandonedCheckout> existing = abandonedCheckoutRepository.findByBookingId(booking.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        AbandonedCheckout record = new AbandonedCheckout();
        record.setBooking(booking);
        record.setClerkUserId(resolveClerkUserId(booking));
        record.setShowId(booking.getShow() != null ? booking.getShow().getId() : null);
        record.setSeatNumbers(booking.getSeatIds());
        record.setAmount(booking.getAmount() != null ? booking.getAmount() : booking.getTotalAmount());
        record.setStatus(AbandonedCheckoutStatus.CHECKOUT_STARTED);
        record.setRecoveryEmailSent(false);
        try {
            AbandonedCheckout saved = abandonedCheckoutRepository.saveAndFlush(record);
            log.info("AbandonedCheckout CHECKOUT_STARTED for booking {} user {}", booking.getId(), saved.getClerkUserId());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // Concurrent create for same booking — return the winner
            return abandonedCheckoutRepository.findByBookingId(booking.getId())
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional
    public AbandonedCheckout markPaymentPending(Long bookingId, String razorpayOrderId) {
        AbandonedCheckout record = loadOrCreate(bookingId);
        if (isTerminal(record)) {
            return record;
        }
        record.setStatus(AbandonedCheckoutStatus.PAYMENT_PENDING);
        if (razorpayOrderId != null && !razorpayOrderId.isBlank()) {
            record.setRazorpayOrderId(razorpayOrderId);
        }
        syncSnapshot(record);
        return abandonedCheckoutRepository.save(record);
    }

    @Transactional
    public AbandonedCheckout markPaymentFailed(Long bookingId) {
        AbandonedCheckout record = loadOrCreate(bookingId);
        if (isTerminal(record)) {
            return record;
        }
        record.setStatus(AbandonedCheckoutStatus.PAYMENT_FAILED);
        record.setEligibleForRecoveryAt(LocalDateTime.now());
        syncSnapshot(record);
        AbandonedCheckout saved = abandonedCheckoutRepository.save(record);
        log.info("AbandonedCheckout PAYMENT_FAILED for booking {} (recoverable)", bookingId);
        return saved;
    }

    @Transactional
    public AbandonedCheckout markPaymentCancelled(Long bookingId) {
        AbandonedCheckout record = loadOrCreate(bookingId);
        if (isTerminal(record)) {
            return record;
        }
        record.setStatus(AbandonedCheckoutStatus.PAYMENT_CANCELLED);
        record.setEligibleForRecoveryAt(LocalDateTime.now());
        syncSnapshot(record);
        AbandonedCheckout saved = abandonedCheckoutRepository.save(record);
        log.info("AbandonedCheckout PAYMENT_CANCELLED for booking {} (recoverable)", bookingId);
        return saved;
    }

    @Transactional
    public AbandonedCheckout markCompleted(Long bookingId) {
        AbandonedCheckout record = loadOrCreate(bookingId);
        record.setStatus(AbandonedCheckoutStatus.COMPLETED);
        record.setEligibleForRecoveryAt(null);
        syncSnapshot(record);
        AbandonedCheckout saved = abandonedCheckoutRepository.save(record);
        log.info("AbandonedCheckout COMPLETED for booking {}", bookingId);
        return saved;
    }

    /**
     * Part 2 entry point: recoverable failed/cancelled checkouts older than recovery delay.
     */
    @Transactional(readOnly = true)
    public List<AbandonedCheckout> findReadyForRecoveryProcessing(LocalDateTime cutoff) {
        return abandonedCheckoutRepository.findReadyForRecoveryProcessing(
                EnumSet.of(AbandonedCheckoutStatus.PAYMENT_FAILED, AbandonedCheckoutStatus.PAYMENT_CANCELLED),
                EnumSet.of(AbandonedCheckoutStatus.CHECKOUT_STARTED, AbandonedCheckoutStatus.PAYMENT_PENDING),
                cutoff);
    }

    /**
     * @deprecated Use {@link #findReadyForRecoveryProcessing(LocalDateTime)} for Part 2 processing.
     */
    @Deprecated
    @Transactional(readOnly = true)
    public List<AbandonedCheckout> findEligibleForRecovery(LocalDateTime olderThan) {
        return abandonedCheckoutRepository.findReadyForRecoveryProcessing(
                EnumSet.of(AbandonedCheckoutStatus.PAYMENT_FAILED, AbandonedCheckoutStatus.PAYMENT_CANCELLED),
                EnumSet.of(AbandonedCheckoutStatus.CHECKOUT_STARTED, AbandonedCheckoutStatus.PAYMENT_PENDING),
                olderThan);
    }

    @Transactional(readOnly = true)
    public Optional<AbandonedCheckout> findByBookingId(Long bookingId) {
        return abandonedCheckoutRepository.findByBookingId(bookingId);
    }

    public boolean isRecoverable(AbandonedCheckout record) {
        return record != null
                && !record.isRecoveryEmailSent()
                && record.getEligibleForRecoveryAt() != null
                && (record.getStatus() == AbandonedCheckoutStatus.PAYMENT_FAILED
                || record.getStatus() == AbandonedCheckoutStatus.PAYMENT_CANCELLED);
    }

    private AbandonedCheckout loadOrCreate(Long bookingId) {
        return abandonedCheckoutRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    Booking booking = bookingRepository.findById(bookingId)
                            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));
                    return trackCheckoutStarted(booking);
                });
    }

    private void syncSnapshot(AbandonedCheckout record) {
        Booking booking = record.getBooking();
        if (booking == null) {
            return;
        }
        if (booking.getSeatIds() != null) {
            record.setSeatNumbers(booking.getSeatIds());
        }
        if (booking.getAmount() != null || booking.getTotalAmount() != null) {
            record.setAmount(booking.getAmount() != null ? booking.getAmount() : booking.getTotalAmount());
        }
        if (booking.getShow() != null) {
            record.setShowId(booking.getShow().getId());
        }
        if (booking.getOrderId() != null) {
            record.setRazorpayOrderId(booking.getOrderId());
        }
        if (record.getClerkUserId() == null || "unknown".equals(record.getClerkUserId())) {
            record.setClerkUserId(resolveClerkUserId(booking));
        }
    }

    private boolean isTerminal(AbandonedCheckout record) {
        return TERMINAL.contains(record.getStatus());
    }

    private String resolveClerkUserId(Booking booking) {
        if (booking.getClerkUserId() != null && !booking.getClerkUserId().isBlank()) {
            return booking.getClerkUserId();
        }
        if (booking.getUser() != null && booking.getUser().getId() != null) {
            return "legacy:" + booking.getUser().getId();
        }
        return "unknown";
    }
}
