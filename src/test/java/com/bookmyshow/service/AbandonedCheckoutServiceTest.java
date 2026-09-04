package com.bookmyshow.service;

import com.bookmyshow.dto.CreateOrderRequest;
import com.bookmyshow.dto.PaymentVerifyRequest;
import com.bookmyshow.entity.AbandonedCheckout;
import com.bookmyshow.entity.AbandonedCheckoutStatus;
import com.bookmyshow.entity.Booking;
import com.bookmyshow.exception.PaymentFailedException;
import com.bookmyshow.repository.AbandonedCheckoutRepository;
import com.bookmyshow.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "razorpay.key-id=test-key-id",
        "razorpay.key-secret=test-secret-value"
})
class AbandonedCheckoutServiceTest {

    @Autowired
    private AbandonedCheckoutService abandonedCheckoutService;

    @Autowired
    private AbandonedCheckoutRepository abandonedCheckoutRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentService paymentService;

    private Booking booking;

    @BeforeEach
    void setUp() {
        authenticateAs("clerk_abandon_1");
        booking = new Booking();
        booking.setClerkUserId("clerk_abandon_1");
        booking.setMovieTitle("Interstellar");
        booking.setSeatIds("A1,A2");
        booking.setAmount(750.00);
        booking.setTotalAmount(750.00);
        booking.setBookingStatus("PENDING_PAYMENT");
        booking.setPaymentStatus("PENDING");
        booking = bookingRepository.save(booking);
    }

    @Test
    void trackCheckoutStarted_createsRecord() {
        AbandonedCheckout record = abandonedCheckoutService.trackCheckoutStarted(booking);

        assertNotNull(record.getId());
        assertEquals(booking.getId(), record.getBooking().getId());
        assertEquals("clerk_abandon_1", record.getClerkUserId());
        assertEquals("A1,A2", record.getSeatNumbers());
        assertEquals(750.00, record.getAmount());
        assertEquals(AbandonedCheckoutStatus.CHECKOUT_STARTED, record.getStatus());
        assertFalse(record.isRecoveryEmailSent());
        assertNotNull(record.getCreatedAt());
        assertNotNull(record.getUpdatedAt());
        assertNull(record.getEligibleForRecoveryAt());
    }

    @Test
    void trackCheckoutStarted_duplicatePrevention_sameBooking() {
        AbandonedCheckout first = abandonedCheckoutService.trackCheckoutStarted(booking);
        AbandonedCheckout second = abandonedCheckoutService.trackCheckoutStarted(booking);

        assertEquals(first.getId(), second.getId());
        assertEquals(1, abandonedCheckoutRepository.findAll().stream()
                .filter(ac -> ac.getBooking().getId().equals(booking.getId()))
                .count());
    }

    @Test
    void markPaymentCancelled_setsRecoverableStatus() {
        abandonedCheckoutService.trackCheckoutStarted(booking);

        AbandonedCheckout record = abandonedCheckoutService.markPaymentCancelled(booking.getId());

        assertEquals(AbandonedCheckoutStatus.PAYMENT_CANCELLED, record.getStatus());
        assertNotNull(record.getEligibleForRecoveryAt());
        assertFalse(record.isRecoveryEmailSent());
        assertTrue(abandonedCheckoutService.isRecoverable(record));
    }

    @Test
    void markPaymentFailed_setsRecoverableStatus() {
        abandonedCheckoutService.trackCheckoutStarted(booking);

        AbandonedCheckout record = abandonedCheckoutService.markPaymentFailed(booking.getId());

        assertEquals(AbandonedCheckoutStatus.PAYMENT_FAILED, record.getStatus());
        assertNotNull(record.getEligibleForRecoveryAt());
        assertTrue(abandonedCheckoutService.isRecoverable(record));
    }

    @Test
    void markCompleted_clearsRecoveryEligibility() {
        abandonedCheckoutService.trackCheckoutStarted(booking);
        abandonedCheckoutService.markPaymentCancelled(booking.getId());

        AbandonedCheckout record = abandonedCheckoutService.markCompleted(booking.getId());

        assertEquals(AbandonedCheckoutStatus.COMPLETED, record.getStatus());
        assertNull(record.getEligibleForRecoveryAt());
        assertFalse(abandonedCheckoutService.isRecoverable(record));
    }

    @Test
    void statusTransitions_checkoutToPendingToFailedToCompleted() {
        AbandonedCheckout started = abandonedCheckoutService.trackCheckoutStarted(booking);
        assertEquals(AbandonedCheckoutStatus.CHECKOUT_STARTED, started.getStatus());

        AbandonedCheckout pending = abandonedCheckoutService.markPaymentPending(booking.getId(), "order_abc");
        assertEquals(AbandonedCheckoutStatus.PAYMENT_PENDING, pending.getStatus());
        assertEquals("order_abc", pending.getRazorpayOrderId());

        AbandonedCheckout failed = abandonedCheckoutService.markPaymentFailed(booking.getId());
        assertEquals(AbandonedCheckoutStatus.PAYMENT_FAILED, failed.getStatus());

        AbandonedCheckout completed = abandonedCheckoutService.markCompleted(booking.getId());
        assertEquals(AbandonedCheckoutStatus.COMPLETED, completed.getStatus());

        // Terminal: further cancel must not reopen
        AbandonedCheckout afterCancel = abandonedCheckoutService.markPaymentCancelled(booking.getId());
        assertEquals(AbandonedCheckoutStatus.COMPLETED, afterCancel.getStatus());
    }

    @Test
    void paymentService_cancelPayment_marksAbandonedCancelled() {
        abandonedCheckoutService.trackCheckoutStarted(booking);

        paymentService.cancelPayment(booking.getId(), "clerk_abandon_1");

        AbandonedCheckout record = abandonedCheckoutService.findByBookingId(booking.getId()).orElseThrow();
        assertEquals(AbandonedCheckoutStatus.PAYMENT_CANCELLED, record.getStatus());
        assertNotNull(record.getEligibleForRecoveryAt());
    }

    @Test
    void paymentService_verifyInvalidSignature_marksAbandonedFailed() {
        abandonedCheckoutService.trackCheckoutStarted(booking);

        PaymentVerifyRequest request = new PaymentVerifyRequest(
                booking.getId(),
                "clerk_abandon_1",
                "order_bad",
                "pay_bad",
                "invalid_signature"
        );

        assertThrows(PaymentFailedException.class, () -> paymentService.verifyPayment(request));

        AbandonedCheckout record = abandonedCheckoutService.findByBookingId(booking.getId()).orElseThrow();
        assertEquals(AbandonedCheckoutStatus.PAYMENT_FAILED, record.getStatus());
        assertNotNull(record.getEligibleForRecoveryAt());
    }

    @Test
    void paymentService_verifyValidSignature_marksAbandonedCompleted() throws Exception {
        abandonedCheckoutService.trackCheckoutStarted(booking);

        String orderId = "order_ok_1";
        String paymentId = "pay_ok_1";
        String secret = "test-secret-value";
        String signature = hmac(orderId + "|" + paymentId, secret);

        PaymentVerifyRequest request = new PaymentVerifyRequest(
                booking.getId(), "clerk_abandon_1", orderId, paymentId, signature);

        paymentService.verifyPayment(request);

        AbandonedCheckout record = abandonedCheckoutService.findByBookingId(booking.getId()).orElseThrow();
        assertEquals(AbandonedCheckoutStatus.COMPLETED, record.getStatus());
        assertNull(record.getEligibleForRecoveryAt());
    }

    @Test
    void paymentService_createOrder_doesNotInventFakeRazorpayOrder() {
        abandonedCheckoutService.trackCheckoutStarted(booking);

        assertThrows(PaymentFailedException.class,
                () -> paymentService.createOrder(new CreateOrderRequest(booking.getId(), "clerk_abandon_1")));

        AbandonedCheckout record = abandonedCheckoutService.findByBookingId(booking.getId()).orElseThrow();
        assertNotEquals(AbandonedCheckoutStatus.PAYMENT_PENDING, record.getStatus());
        assertTrue(record.getRazorpayOrderId() == null || !record.getRazorpayOrderId().startsWith("order_eval_"));
    }

    private static String hmac(String payload, String secret) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec spec =
                new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(spec);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }

    private void authenticateAs(String clerkUserId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        clerkUserId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
