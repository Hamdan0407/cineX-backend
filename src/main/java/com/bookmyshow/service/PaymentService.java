package com.bookmyshow.service;

import com.bookmyshow.dto.*;
import com.bookmyshow.dto.mapper.PaymentMapper;
import com.bookmyshow.entity.Booking;
import com.bookmyshow.entity.Payment;
import com.bookmyshow.entity.WalletReferenceType;
import com.bookmyshow.exception.PaymentFailedException;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.repository.BookingRepository;
import com.bookmyshow.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private TicketService ticketService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private EmailService emailService;

    private String getEffectiveKeyId() {
        if (keyId == null || keyId.trim().isEmpty()) {
            throw new IllegalStateException("Razorpay key ID is not configured");
        }
        return keyId;
    }

    private String getEffectiveKeySecret() {
        if (keySecret == null || keySecret.trim().isEmpty()) {
            throw new IllegalStateException("Razorpay key secret is not configured");
        }
        return keySecret;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.bookmyshow.monitoring.CineXMetricsService metricsService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private SeatLockService seatLockService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AbandonedCheckoutService abandonedCheckoutService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.bookmyshow.repository.BookingSeatRepository bookingSeatRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private WalletService walletService;

    private final AdminAuthService adminAuthService;

    public PaymentService(PaymentRepository paymentRepository, BookingRepository bookingRepository,
                          AdminAuthService adminAuthService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.adminAuthService = adminAuthService;
    }

    /**
     * Step 1: Create Razorpay Order
     * Calculates amount only on backend, never trusts frontend amount.
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + request.getBookingId()));

        adminAuthService.validateBookingAccess(booking);

        // Calculate amount only on backend
        Double bookingAmount = booking.getAmount() != null ? booking.getAmount() : booking.getTotalAmount();
        if (bookingAmount == null || bookingAmount <= 0) {
            throw new IllegalArgumentException("Invalid booking amount in database for Booking ID: " + booking.getId());
        }

        long amountInPaise = Math.round(bookingAmount * 100);
        String effectiveKeyId = getEffectiveKeyId();
        String effectiveKeySecret = getEffectiveKeySecret();
        String orderId;

        try {
            RazorpayClient razorpayClient = new RazorpayClient(effectiveKeyId, effectiveKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + booking.getId());

            Order order = razorpayClient.orders.create(orderRequest);
            orderId = order.get("id");
            log.info("Created Razorpay Test Order {} for Booking ID {}", orderId, booking.getId());
        } catch (Exception e) {
            log.error("Razorpay order creation failed for booking {}: {}", booking.getId(), e.getMessage());
            throw new PaymentFailedException("Unable to create Razorpay order. Please try again.");
        }

        // Store Order ID and update status to PENDING_PAYMENT
        booking.setOrderId(orderId);
        booking.setPaymentStatus("PENDING");
        booking.setBookingStatus("PENDING_PAYMENT");
        bookingRepository.save(booking);

        Payment payment = new Payment();
        payment.setBookingId(booking.getId());
        payment.setAmount(bookingAmount);
        payment.setStatus("PENDING");
        payment.setOrderId(orderId);
        paymentRepository.save(payment);

        if (abandonedCheckoutService != null) {
            abandonedCheckoutService.markPaymentPending(booking.getId(), orderId);
        }

        if (metricsService != null) {
            metricsService.recordBookingAttempt();
        }

        return new OrderResponse(
                orderId,
                booking.getId(),
                amountInPaise,
                bookingAmount,
                "INR",
                effectiveKeyId,
                "CineX",
                "Movie Ticket - " + (booking.getMovieTitle() != null ? booking.getMovieTitle() : "Cinema"),
                booking.getMovieTitle(),
                null
        );
    }

    /**
     * Step 3: Verify Razorpay Signature using HMAC SHA256
     * Never trusts payment success from frontend.
     */
    @org.springframework.transaction.annotation.Transactional
    public PaymentResponse verifyPayment(PaymentVerifyRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + request.getBookingId()));

        adminAuthService.validateBookingAccess(booking);

        if ("BOOKED".equals(booking.getBookingStatus())
                && request.getRazorpayPaymentId() != null
                && request.getRazorpayPaymentId().equals(booking.getPaymentId())) {
            PaymentResponse replay = new PaymentResponse("SUCCESS", request.getRazorpayPaymentId(),
                    request.getRazorpayOrderId(), booking.getId(), "Payment already verified");
            replay.setTicketToken(booking.getTicketToken());
            return replay;
        }

        boolean isSignatureValid = false;
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.getRazorpayOrderId());
            attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
            attributes.put("razorpay_signature", request.getRazorpaySignature());
            isSignatureValid = Utils.verifyPaymentSignature(attributes, getEffectiveKeySecret());
        } catch (Exception e) {
            log.warn("Utils.verifyPaymentSignature check threw exception, checking manual HMAC SHA256: {}", e.getMessage());
        }

        if (!isSignatureValid) {
            String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
            isSignatureValid = verifyHmacSha256(payload, request.getRazorpaySignature(), getEffectiveKeySecret());
        }

        if (!isSignatureValid) {
            log.error("HMAC SHA256 signature verification failed for Order {} and Payment {}", request.getRazorpayOrderId(), request.getRazorpayPaymentId());
            booking.setPaymentStatus("FAILED");
            booking.setBookingStatus("FAILED");
            bookingRepository.save(booking);

            Payment payment = new Payment();
            payment.setBookingId(booking.getId());
            payment.setAmount(booking.getAmount() != null ? booking.getAmount() : booking.getTotalAmount());
            payment.setStatus("FAILED");
            payment.setOrderId(request.getRazorpayOrderId());
            payment.setTransactionId(request.getRazorpayPaymentId());
            payment.setSignature(request.getRazorpaySignature());
            paymentRepository.save(payment);

            if (metricsService != null) {
                metricsService.recordFailedBooking();
            }

            if (abandonedCheckoutService != null) {
                abandonedCheckoutService.markPaymentFailed(booking.getId());
            }

            throw new PaymentFailedException("Payment verification failed: Invalid Razorpay HMAC SHA256 signature");
        }

        log.info("Payment successfully verified for Booking {} with Payment ID {}", booking.getId(), request.getRazorpayPaymentId());
        return finalizeSuccessfulPayment(booking, request.getRazorpayPaymentId(), request.getRazorpayOrderId(), request.getRazorpaySignature(), "Payment verified successfully");
    }

    /** Wallet checkout is atomic: validate held seats, debit the verified owner's wallet, then finalize normally. */
    @org.springframework.transaction.annotation.Transactional
    public PaymentResponse payWithWallet(Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));
        adminAuthService.validateBookingAccess(booking);

        if ("BOOKED".equals(booking.getBookingStatus()) && "WALLET".equals(booking.getPaymentId())) {
            PaymentResponse replay = new PaymentResponse("SUCCESS", "WALLET", null, booking.getId(), "Wallet payment already completed");
            replay.setTicketToken(booking.getTicketToken());
            return replay;
        }
        if (!"PENDING_PAYMENT".equals(booking.getBookingStatus()) || !"PENDING".equals(booking.getPaymentStatus())) {
            throw new PaymentFailedException("This booking is no longer available for wallet payment");
        }
        if (booking.getShow() == null || bookingSeatRepository == null || seatLockService == null) {
            throw new PaymentFailedException("Unable to verify the seat hold for this booking");
        }
        java.util.List<com.bookmyshow.entity.BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(booking.getId());
        java.util.List<Long> seatIds = bookingSeats.stream().map(bs -> bs.getSeat().getId()).toList();
        if (seatIds.isEmpty() || !seatLockService.areSeatsHeldBy(booking.getShow().getId(), seatIds, booking.getClerkUserId())) {
            throw new PaymentFailedException("Your seat hold has expired. Please select your seats again.");
        }
        Double total = booking.getAmount() != null ? booking.getAmount() : booking.getTotalAmount();
        if (total == null || total <= 0) throw new PaymentFailedException("Invalid booking total");

        walletService.debitWallet(booking.getClerkUserId(), BigDecimal.valueOf(total), WalletReferenceType.BOOKING,
                "booking-" + booking.getId(), "Movie Booking - Booking #" + booking.getId());
        return finalizeSuccessfulPayment(booking, "WALLET", null, null, "Wallet payment completed successfully");
    }

    private PaymentResponse finalizeSuccessfulPayment(Booking booking, String transactionId, String orderId, String signature, String message) {
        booking.setPaymentStatus("SUCCESS");
        booking.setBookingStatus("BOOKED");
        booking.setPaymentId(transactionId);
        booking.setSignature(signature);
        if (booking.getTicketToken() == null) booking.setTicketToken(UUID.randomUUID().toString());
        bookingRepository.save(booking);

        Payment payment = new Payment();
        payment.setBookingId(booking.getId());
        payment.setAmount(booking.getAmount() != null ? booking.getAmount() : booking.getTotalAmount());
        payment.setStatus("SUCCESS");
        payment.setOrderId(orderId);
        payment.setTransactionId(transactionId);
        payment.setSignature(signature);
        paymentRepository.save(payment);

        if (abandonedCheckoutService != null) {
            abandonedCheckoutService.markCompleted(booking.getId());
        }

        if (metricsService != null) {
            metricsService.recordSuccessfulBooking(java.math.BigDecimal.valueOf(payment.getAmount()));
            metricsService.recordTicketGenerated();
        }

        if (bookingSeatRepository != null) {
            java.util.List<com.bookmyshow.entity.BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(booking.getId());
            if (bookingSeats != null && !bookingSeats.isEmpty()) {
                bookingSeats.forEach(bs -> bs.setStatus("BOOKED"));
                bookingSeatRepository.saveAll(bookingSeats);
                if (seatLockService != null && booking.getShow() != null) {
                    java.util.List<Long> sIds = bookingSeats.stream().map(bs -> bs.getSeat().getId()).collect(java.util.stream.Collectors.toList());
                    seatLockService.broadcastSeatBooked(booking.getShow().getId(), sIds, booking.getClerkUserId() != null ? booking.getClerkUserId() : "VERIFIED_USER");
                }
            }
        }

        if (booking.getUserEmail() != null && !booking.getUserEmail().trim().isEmpty() && ticketService != null && emailService != null) {
            try {
                String qrBase64 = ticketService.generateQrCodeBase64(booking.getTicketToken());
                emailService.sendHtmlTicketConfirmation(booking.getUserEmail(), booking, qrBase64, booking.getTicketToken());
            } catch (Exception e) {
                log.error("Failed to send HTML ticket email: {}", e.getMessage());
            }
        }

        PaymentResponse response = new PaymentResponse("SUCCESS", transactionId, orderId, booking.getId(), message);
        response.setTicketToken(booking.getTicketToken());
        return response;
    }

    /**
     * Handle payment cancellation or timeout
     */
    public void cancelPayment(Long bookingId, String authenticatedClerkUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        adminAuthService.validateBookingAccess(booking);

        if (!"BOOKED".equals(booking.getBookingStatus())) {
            log.info("Cancelling payment/booking for ID {}", bookingId);
            booking.setPaymentStatus("FAILED");
            booking.setBookingStatus("CANCELLED");
            bookingRepository.save(booking);

            if (bookingSeatRepository != null) {
                java.util.List<com.bookmyshow.entity.BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
                if (bookingSeats != null && !bookingSeats.isEmpty()) {
                    bookingSeats.forEach(bs -> {
                        if (!"BOOKED".equals(bs.getStatus())) {
                            bs.setStatus("CANCELLED");
                        }
                    });
                    bookingSeatRepository.saveAll(bookingSeats);
                    if (seatLockService != null && booking.getShow() != null) {
                        java.util.List<Long> seatIds = bookingSeats.stream()
                                .map(bs -> bs.getSeat().getId())
                                .collect(java.util.stream.Collectors.toList());
                        // The booking has already passed ownership/admin authorization above. Release the
                        // associated hold regardless of whether the caller is an administrator or owner.
                        seatLockService.releaseSeats(booking.getShow().getId(), seatIds, null, null);
                    }
                }
            }

            if (abandonedCheckoutService != null) {
                abandonedCheckoutService.markPaymentCancelled(bookingId);
            }
        }
    }

    private boolean verifyHmacSha256(String payload, String expectedSignature, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equals(expectedSignature);
        } catch (Exception e) {
            log.error("Error computing HMAC SHA256: {}", e.getMessage());
            return false;
        }
    }


    public List<PaymentDto> getPaymentsByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId).stream()
                .map(PaymentMapper::toResponse)
                .collect(Collectors.toList());
    }
}
