package com.bookmyshow.service;

import com.bookmyshow.config.AbandonedCheckoutProperties;
import com.bookmyshow.dto.AbandonedCheckoutRecoveryEmailContent;
import com.bookmyshow.entity.AbandonedCheckout;
import com.bookmyshow.entity.AbandonedCheckoutStatus;
import com.bookmyshow.entity.Booking;
import com.bookmyshow.entity.User;
import com.bookmyshow.repository.AbandonedCheckoutRepository;
import com.bookmyshow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AbandonedCheckoutRecoveryProcessor {

    private static final Set<AbandonedCheckoutStatus> RECOVERABLE_STATUSES = EnumSet.of(
            AbandonedCheckoutStatus.PAYMENT_FAILED,
            AbandonedCheckoutStatus.PAYMENT_CANCELLED,
            AbandonedCheckoutStatus.CHECKOUT_STARTED,
            AbandonedCheckoutStatus.PAYMENT_PENDING
    );

    private final AbandonedCheckoutProperties properties;
    private final AbandonedCheckoutService abandonedCheckoutService;
    private final AbandonedCheckoutRepository abandonedCheckoutRepository;
    private final AbandonedCheckoutRecoveryUrlBuilder recoveryUrlBuilder;
    private final AbandonedCheckoutRecoveryEmailBuilder recoveryEmailBuilder;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Transactional
    public RecoveryRunSummary processEligibleCheckouts() {
        LocalDateTime cutoff = LocalDateTime.now().minus(properties.getRecoveryDelay());
        List<AbandonedCheckout> candidates = abandonedCheckoutService.findReadyForRecoveryProcessing(cutoff);

        int sent = 0;
        int skipped = 0;
        int failed = 0;

        for (AbandonedCheckout candidate : candidates) {
            RecoveryAttemptResult result = processOne(candidate.getId());
            switch (result) {
                case SENT -> sent++;
                case FAILED -> failed++;
                case SKIPPED -> skipped++;
            }
        }

        if (sent > 0 || failed > 0) {
            log.info("Abandoned checkout recovery run complete: sent={}, skipped={}, failed={}, scanned={}",
                    sent, skipped, failed, candidates.size());
        }

        return new RecoveryRunSummary(candidates.size(), sent, skipped, failed);
    }

    @Transactional
    public RecoveryAttemptResult processOne(Long abandonedCheckoutId) {
        AbandonedCheckout record = abandonedCheckoutRepository.findByIdForUpdate(abandonedCheckoutId).orElse(null);
        if (record == null) {
            return RecoveryAttemptResult.SKIPPED;
        }

        if (!isStillEligible(record, LocalDateTime.now().minus(properties.getRecoveryDelay()))) {
            return RecoveryAttemptResult.SKIPPED;
        }

        Booking booking = record.getBooking();
        if (booking == null) {
            log.warn("Skipping abandoned checkout {} because booking is missing", abandonedCheckoutId);
            return RecoveryAttemptResult.SKIPPED;
        }

        if (isBookingCompleted(booking)) {
            abandonedCheckoutService.markCompleted(booking.getId());
            log.info("Skipping abandoned checkout {} because booking {} is already completed", abandonedCheckoutId, booking.getId());
            return RecoveryAttemptResult.SKIPPED;
        }

        String recipientEmail = resolveRecipientEmail(booking);
        if (recipientEmail == null) {
            log.info("Skipping abandoned checkout {} because no valid recipient email is available", abandonedCheckoutId);
            return RecoveryAttemptResult.SKIPPED;
        }

        String recipientName = resolveRecipientName(booking);
        AbandonedCheckoutRecoveryEmailContent emailContent = AbandonedCheckoutRecoveryEmailContent.builder()
                .recipientName(recipientName)
                .movieTitle(defaultText(booking.getMovieTitle(), "Your selected movie"))
                .theatreName(defaultText(booking.getTheatreName(), "CineX Theatre"))
                .cityName(defaultText(booking.getCityName(), "Your city"))
                .showDate(defaultText(booking.getShowDate(), "Upcoming show"))
                .showTime(defaultText(booking.getShowTime(), "Showtime TBA"))
                .seatNumbers(defaultText(record.getSeatNumbers() != null ? record.getSeatNumbers() : booking.getSeatIds(), "Selected seats"))
                .amount(formatAmount(record.getAmount() != null ? record.getAmount() : booking.getTotalAmount()))
                .recoveryUrl(recoveryUrlBuilder.buildRecoveryUrl(booking))
                .build();

        try {
            emailService.sendAbandonedCheckoutRecoveryEmail(
                    recipientEmail,
                    emailContent,
                    recoveryEmailBuilder.buildHtml(emailContent),
                    recoveryEmailBuilder.buildPlainText(emailContent)
            );
        } catch (Exception ex) {
            log.warn("Recovery email failed for abandoned checkout {} (booking {}): {}",
                    abandonedCheckoutId, booking.getId(), ex.getMessage());
            return RecoveryAttemptResult.FAILED;
        }

        int updated = abandonedCheckoutRepository.markRecoverySentIfEligible(
                abandonedCheckoutId,
                LocalDateTime.now(),
                RECOVERABLE_STATUSES);

        if (updated == 0) {
            log.info("Recovery email already processed for abandoned checkout {}", abandonedCheckoutId);
            return RecoveryAttemptResult.SKIPPED;
        }

        log.info("Recovery email sent for abandoned checkout {} to {}", abandonedCheckoutId, maskEmail(recipientEmail));
        return RecoveryAttemptResult.SENT;
    }

    boolean isStillEligible(AbandonedCheckout record, LocalDateTime cutoff) {
        if (record == null || record.isRecoveryEmailSent()) {
            return false;
        }
        AbandonedCheckoutStatus status = record.getStatus();
        if (status == AbandonedCheckoutStatus.COMPLETED || status == AbandonedCheckoutStatus.RECOVERY_SENT) {
            return false;
        }
        if (status == AbandonedCheckoutStatus.PAYMENT_FAILED || status == AbandonedCheckoutStatus.PAYMENT_CANCELLED) {
            return record.getEligibleForRecoveryAt() != null && !record.getEligibleForRecoveryAt().isAfter(cutoff);
        }
        if (status == AbandonedCheckoutStatus.CHECKOUT_STARTED || status == AbandonedCheckoutStatus.PAYMENT_PENDING) {
            return record.getUpdatedAt() != null && !record.getUpdatedAt().isAfter(cutoff);
        }
        return false;
    }

    boolean isBookingCompleted(Booking booking) {
        if (booking == null) {
            return true;
        }
        if ("BOOKED".equalsIgnoreCase(booking.getBookingStatus())) {
            return true;
        }
        if ("SUCCESS".equalsIgnoreCase(booking.getPaymentStatus())) {
            return true;
        }
        return "CONFIRMED".equalsIgnoreCase(booking.getStatus());
    }

    String resolveRecipientEmail(Booking booking) {
        if (booking.getUserEmail() != null && isValidEmail(booking.getUserEmail())) {
            return booking.getUserEmail().trim();
        }
        if (booking.getClerkUserId() != null) {
            return userRepository.findByClerkUserId(booking.getClerkUserId())
                    .map(User::getEmail)
                    .filter(this::isValidEmail)
                    .map(String::trim)
                    .orElse(null);
        }
        if (booking.getUser() != null && isValidEmail(booking.getUser().getEmail())) {
            return booking.getUser().getEmail().trim();
        }
        return null;
    }

    String resolveRecipientName(Booking booking) {
        if (booking.getClerkUserId() != null) {
            String name = userRepository.findByClerkUserId(booking.getClerkUserId())
                    .map(User::getName)
                    .filter(this::hasText)
                    .orElse(null);
            if (name != null) {
                return name;
            }
        }
        if (booking.getUser() != null && hasText(booking.getUser().getName())) {
            return booking.getUser().getName();
        }
        return "there";
    }

    private boolean isValidEmail(String email) {
        if (!hasText(email)) {
            return false;
        }
        return email.contains("@") && email.contains(".");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private String formatAmount(Double amount) {
        return amount == null || amount <= 0 ? "Amount confirmed at checkout" : "Rs. " + Math.round(amount);
    }

    static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "hidden";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) {
            return "**@" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }

    public enum RecoveryAttemptResult {
        SENT, SKIPPED, FAILED
    }

    public record RecoveryRunSummary(int scanned, int sent, int skipped, int failed) {
    }
}
