package com.bookmyshow.service;

import com.bookmyshow.dto.AbandonedCheckoutRecoveryEmailContent;
import com.bookmyshow.entity.AbandonedCheckout;
import com.bookmyshow.entity.AbandonedCheckoutStatus;
import com.bookmyshow.entity.Booking;
import com.bookmyshow.entity.User;
import com.bookmyshow.repository.AbandonedCheckoutRepository;
import com.bookmyshow.repository.BookingRepository;
import com.bookmyshow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "abandoned-checkout.recovery-delay-minutes=30",
        "abandoned-checkout.processor-enabled=false",
        "abandoned-checkout.frontend-url=https://cinex.example.com"
})
class AbandonedCheckoutRecoveryProcessorTest {

    @Autowired
    private AbandonedCheckoutRecoveryProcessor recoveryProcessor;

    @Autowired
    private AbandonedCheckoutService abandonedCheckoutService;

    @Autowired
    private AbandonedCheckoutRepository abandonedCheckoutRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    void resetEmailMock() {
        reset(emailService);
        doNothing().when(emailService).sendAbandonedCheckoutRecoveryEmail(
                anyString(), any(), anyString(), anyString());
    }

    @Test
    void processOne_sendsEmailForEligibleCancelledCheckout() {
        Booking booking = saveBooking("clerk_recover_1", "user@cinex.test", "PENDING_PAYMENT", "FAILED");
        abandonedCheckoutService.trackCheckoutStarted(booking);
        abandonedCheckoutService.markPaymentCancelled(booking.getId());

        AbandonedCheckout record = abandonedCheckoutRepository.findByBookingId(booking.getId()).orElseThrow();
        record.setEligibleForRecoveryAt(LocalDateTime.now().minusMinutes(45));
        abandonedCheckoutRepository.save(record);

        AbandonedCheckoutRecoveryProcessor.RecoveryAttemptResult result =
                recoveryProcessor.processOne(record.getId());

        assertEquals(AbandonedCheckoutRecoveryProcessor.RecoveryAttemptResult.SENT, result);
        verify(emailService, times(1)).sendAbandonedCheckoutRecoveryEmail(
                eq("user@cinex.test"),
                argThat(content -> content.getMovieTitle().equals("Inception")
                        && content.getCityName().equals("Chennai")
                        && content.getSeatNumbers().equals("A1,A2")
                        && content.getAmount().equals("Rs. 500")
                        && content.getRecoveryUrl().contains("recover=1")),
                anyString(),
                anyString());

        AbandonedCheckout updated = abandonedCheckoutRepository.findById(record.getId()).orElseThrow();
        assertEquals(AbandonedCheckoutStatus.RECOVERY_SENT, updated.getStatus());
        assertTrue(updated.isRecoveryEmailSent());
        assertNotNull(updated.getRecoveryEmailSentAt());
    }

    @Test
    void processOne_skipsCheckoutYoungerThanRecoveryDelay() {
        Booking booking = saveBooking("clerk_recover_2", "user@cinex.test", "PENDING_PAYMENT", "FAILED");
        abandonedCheckoutService.trackCheckoutStarted(booking);
        abandonedCheckoutService.markPaymentCancelled(booking.getId());

        AbandonedCheckout record = abandonedCheckoutRepository.findByBookingId(booking.getId()).orElseThrow();
        record.setEligibleForRecoveryAt(LocalDateTime.now().minusMinutes(5));
        abandonedCheckoutRepository.save(record);

        assertEquals(AbandonedCheckoutRecoveryProcessor.RecoveryAttemptResult.SKIPPED,
                recoveryProcessor.processOne(record.getId()));
        verify(emailService, never()).sendAbandonedCheckoutRecoveryEmail(anyString(), any(), anyString(), anyString());
    }

    @Test
    void processOne_skipsCompletedBooking() {
        Booking booking = saveBooking("clerk_recover_3", "user@cinex.test", "BOOKED", "SUCCESS");
        abandonedCheckoutService.trackCheckoutStarted(booking);
        AbandonedCheckout record = abandonedCheckoutRepository.findByBookingId(booking.getId()).orElseThrow();
        record.setStatus(AbandonedCheckoutStatus.PAYMENT_CANCELLED);
        record.setEligibleForRecoveryAt(LocalDateTime.now().minusMinutes(60));
        abandonedCheckoutRepository.save(record);

        assertEquals(AbandonedCheckoutRecoveryProcessor.RecoveryAttemptResult.SKIPPED,
                recoveryProcessor.processOne(record.getId()));
        verify(emailService, never()).sendAbandonedCheckoutRecoveryEmail(anyString(), any(), anyString(), anyString());

        AbandonedCheckout updated = abandonedCheckoutRepository.findById(record.getId()).orElseThrow();
        assertEquals(AbandonedCheckoutStatus.COMPLETED, updated.getStatus());
    }

    @Test
    void processOne_skipsWhenEmailMissing() {
        Booking booking = saveBooking("clerk_recover_4", null, "PENDING_PAYMENT", "FAILED");
        abandonedCheckoutService.trackCheckoutStarted(booking);
        abandonedCheckoutService.markPaymentFailed(booking.getId());

        AbandonedCheckout record = abandonedCheckoutRepository.findByBookingId(booking.getId()).orElseThrow();
        record.setEligibleForRecoveryAt(LocalDateTime.now().minusMinutes(60));
        abandonedCheckoutRepository.save(record);

        assertEquals(AbandonedCheckoutRecoveryProcessor.RecoveryAttemptResult.SKIPPED,
                recoveryProcessor.processOne(record.getId()));
        verify(emailService, never()).sendAbandonedCheckoutRecoveryEmail(anyString(), any(), anyString(), anyString());
        assertFalse(record.isRecoveryEmailSent());
    }

    @Test
    void processOne_retriesAfterEmailFailure() {
        Booking booking = saveBooking("clerk_recover_5", "retry@cinex.test", "PENDING_PAYMENT", "FAILED");
        abandonedCheckoutService.trackCheckoutStarted(booking);
        abandonedCheckoutService.markPaymentFailed(booking.getId());
        AbandonedCheckout record = abandonedCheckoutRepository.findByBookingId(booking.getId()).orElseThrow();
        record.setEligibleForRecoveryAt(LocalDateTime.now().minusMinutes(60));
        abandonedCheckoutRepository.save(record);

        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (attempts.getAndIncrement() == 0) {
                throw new com.bookmyshow.exception.EmailDeliveryException("SMTP unavailable");
            }
            return null;
        }).when(emailService).sendAbandonedCheckoutRecoveryEmail(anyString(), any(), anyString(), anyString());

        assertEquals(AbandonedCheckoutRecoveryProcessor.RecoveryAttemptResult.FAILED,
                recoveryProcessor.processOne(record.getId()));
        assertEquals(AbandonedCheckoutStatus.PAYMENT_FAILED,
                abandonedCheckoutRepository.findById(record.getId()).orElseThrow().getStatus());

        assertEquals(AbandonedCheckoutRecoveryProcessor.RecoveryAttemptResult.SENT,
                recoveryProcessor.processOne(record.getId()));
        assertEquals(AbandonedCheckoutStatus.RECOVERY_SENT,
                abandonedCheckoutRepository.findById(record.getId()).orElseThrow().getStatus());
    }

    @Test
    void processOne_preventsDuplicateEmails() {
        Booking booking = saveBooking("clerk_recover_6", "once@cinex.test", "PENDING_PAYMENT", "FAILED");
        abandonedCheckoutService.trackCheckoutStarted(booking);
        abandonedCheckoutService.markPaymentCancelled(booking.getId());
        AbandonedCheckout record = abandonedCheckoutRepository.findByBookingId(booking.getId()).orElseThrow();
        record.setEligibleForRecoveryAt(LocalDateTime.now().minusMinutes(60));
        abandonedCheckoutRepository.save(record);

        assertEquals(AbandonedCheckoutRecoveryProcessor.RecoveryAttemptResult.SENT,
                recoveryProcessor.processOne(record.getId()));
        assertEquals(AbandonedCheckoutRecoveryProcessor.RecoveryAttemptResult.SKIPPED,
                recoveryProcessor.processOne(record.getId()));

        verify(emailService, times(1)).sendAbandonedCheckoutRecoveryEmail(anyString(), any(), anyString(), anyString());
    }

    @Test
    void processOne_resolvesEmailAndNameFromUserProfile() {
        User user = new User();
        user.setClerkUserId("clerk_profile_1");
        user.setEmail("profile@cinex.test");
        user.setName("Aisha");
        userRepository.save(user);

        Booking booking = saveBooking("clerk_profile_1", null, "PENDING_PAYMENT", "FAILED");
        abandonedCheckoutService.trackCheckoutStarted(booking);
        abandonedCheckoutService.markPaymentCancelled(booking.getId());
        AbandonedCheckout record = abandonedCheckoutRepository.findByBookingId(booking.getId()).orElseThrow();
        record.setEligibleForRecoveryAt(LocalDateTime.now().minusMinutes(60));
        abandonedCheckoutRepository.save(record);

        recoveryProcessor.processOne(record.getId());

        verify(emailService).sendAbandonedCheckoutRecoveryEmail(
                eq("profile@cinex.test"),
                argThat(content -> "Aisha".equals(content.getRecipientName())),
                anyString(),
                anyString());
    }

    @Test
    void processEligibleCheckouts_runsBatch() {
        Booking booking = saveBooking("clerk_recover_batch", "batch@cinex.test", "PENDING_PAYMENT", "FAILED");
        abandonedCheckoutService.trackCheckoutStarted(booking);
        abandonedCheckoutService.markPaymentCancelled(booking.getId());
        AbandonedCheckout record = abandonedCheckoutRepository.findByBookingId(booking.getId()).orElseThrow();
        record.setEligibleForRecoveryAt(LocalDateTime.now().minusMinutes(60));
        abandonedCheckoutRepository.save(record);

        AbandonedCheckoutRecoveryProcessor.RecoveryRunSummary summary = recoveryProcessor.processEligibleCheckouts();

        assertEquals(1, summary.scanned());
        assertEquals(1, summary.sent());
        assertEquals(0, summary.failed());
    }

    private Booking saveBooking(String clerkUserId, String email, String bookingStatus, String paymentStatus) {
        Booking booking = new Booking();
        booking.setClerkUserId(clerkUserId);
        booking.setUserEmail(email);
        booking.setMovieId(101L);
        booking.setMovieTitle("Inception");
        booking.setTheatreName("PVR Sathyam");
        booking.setCityName("Chennai");
        booking.setShowDate("12 Sep 2026");
        booking.setShowTime("7:30 PM");
        booking.setSeatIds("A1,A2");
        booking.setAmount(500.0);
        booking.setTotalAmount(500.0);
        booking.setBookingStatus(bookingStatus);
        booking.setPaymentStatus(paymentStatus);
        return bookingRepository.save(booking);
    }
}
