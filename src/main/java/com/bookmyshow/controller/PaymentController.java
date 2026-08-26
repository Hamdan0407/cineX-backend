package com.bookmyshow.controller;

import com.bookmyshow.dto.*;
import com.bookmyshow.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Tag(name = "Payment API", description = "Operations related to Razorpay payment lifecycle")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Create Razorpay order for booking")
    @PostMapping("/create-order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(paymentService.createOrder(request));
    }

    @Operation(summary = "Verify Razorpay payment signature")
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(@Valid @RequestBody PaymentVerifyRequest request) {
        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }

    @Operation(summary = "Cancel payment or handle timeout")
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, String>> cancelPayment(@RequestParam Long bookingId, @RequestParam(required = false) String clerkUserId) {
        paymentService.cancelPayment(bookingId, clerkUserId);
        return ResponseEntity.ok(Map.of("status", "CANCELLED", "message", "Payment cancelled"));
    }

    @Operation(summary = "Get payments by booking ID")
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<PaymentDto>> getPaymentsByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentsByBookingId(bookingId));
    }
}
