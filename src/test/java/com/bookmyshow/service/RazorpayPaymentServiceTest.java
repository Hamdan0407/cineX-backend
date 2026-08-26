package com.bookmyshow.service;

import com.bookmyshow.dto.CreateOrderRequest;
import com.bookmyshow.dto.OrderResponse;
import com.bookmyshow.dto.PaymentResponse;
import com.bookmyshow.dto.PaymentVerifyRequest;
import com.bookmyshow.entity.Booking;
import com.bookmyshow.exception.PaymentFailedException;
import com.bookmyshow.repository.BookingRepository;
import com.bookmyshow.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "razorpay.key-id=rzp_test_TAG6wbNf35AKL4",
        "razorpay.key-secret=44muwJ8rF923cVwbmfY06skP"
})
public class RazorpayPaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private Long testBookingId;

    @BeforeEach
    void setUp() {
        Booking booking = new Booking();
        booking.setClerkUserId("clerk_test_123");
        booking.setMovieTitle("Inception");
        booking.setAmount(500.00);
        booking.setTotalAmount(500.00);
        booking.setBookingStatus("PENDING_PAYMENT");
        booking.setPaymentStatus("PENDING");
        booking = bookingRepository.save(booking);
        testBookingId = booking.getId();
    }

    @Test
    void testCreateOrder_SecurityCheck_UnauthorizedUser_ThrowsException() {
        CreateOrderRequest request = new CreateOrderRequest(testBookingId, "clerk_hacker_999");
        assertThrows(SecurityException.class, () -> paymentService.createOrder(request));
    }

    @Test
    void testVerifyPayment_ValidSignature_Success() throws Exception {
        String orderId = "order_test_123456";
        String paymentId = "pay_test_987654";
        String secret = "44muwJ8rF923cVwbmfY06skP";

        // Compute valid HMAC SHA256 signature
        String payload = orderId + "|" + paymentId;
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec spec = new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(spec);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String validSignature = hexString.toString();

        PaymentVerifyRequest request = new PaymentVerifyRequest(testBookingId, "clerk_test_123", orderId, paymentId, validSignature);
        PaymentResponse response = paymentService.verifyPayment(request);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(paymentId, response.getTransactionId());
        assertEquals(orderId, response.getOrderId());
        assertEquals(testBookingId, response.getBookingId());

        Booking updatedBooking = bookingRepository.findById(testBookingId).orElseThrow();
        assertEquals("BOOKED", updatedBooking.getBookingStatus());
        assertEquals("SUCCESS", updatedBooking.getPaymentStatus());
        assertEquals(paymentId, updatedBooking.getPaymentId());
        assertEquals(validSignature, updatedBooking.getSignature());
    }

    @Test
    void testVerifyPayment_InvalidSignature_ThrowsPaymentFailedException() {
        PaymentVerifyRequest request = new PaymentVerifyRequest(
                testBookingId,
                "clerk_test_123",
                "order_test_123456",
                "pay_test_987654",
                "invalid_fake_signature_hex"
        );

        assertThrows(PaymentFailedException.class, () -> paymentService.verifyPayment(request));

        Booking updatedBooking = bookingRepository.findById(testBookingId).orElseThrow();
        assertEquals("FAILED", updatedBooking.getBookingStatus());
        assertEquals("FAILED", updatedBooking.getPaymentStatus());
    }

    @Test
    void testCancelPayment_Success() {
        paymentService.cancelPayment(testBookingId, "clerk_test_123");

        Booking updatedBooking = bookingRepository.findById(testBookingId).orElseThrow();
        assertEquals("CANCELLED", updatedBooking.getBookingStatus());
        assertEquals("FAILED", updatedBooking.getPaymentStatus());
    }

    @Test
    void testDuplicateVerificationRequest_IdempotentBehavior() throws Exception {
        String orderId = "order_test_123456";
        String paymentId = "pay_test_987654";
        String secret = "44muwJ8rF923cVwbmfY06skP";

        String payload = orderId + "|" + paymentId;
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec spec = new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(spec);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String validSignature = hexString.toString();

        PaymentVerifyRequest request = new PaymentVerifyRequest(testBookingId, "clerk_test_123", orderId, paymentId, validSignature);
        
        // First verification
        PaymentResponse res1 = paymentService.verifyPayment(request);
        assertEquals("SUCCESS", res1.getStatus());

        // Second duplicate verification request
        PaymentResponse res2 = paymentService.verifyPayment(request);
        assertEquals("SUCCESS", res2.getStatus());
    }
}
