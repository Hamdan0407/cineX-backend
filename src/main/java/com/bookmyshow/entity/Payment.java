package com.bookmyshow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long bookingId;
    private Double amount;
    private String status;
    private String transactionId; // Razorpay payment_id
    private String orderId;       // Razorpay order_id
    private String signature;     // Razorpay signature
    private LocalDateTime createdAt = LocalDateTime.now();
}
