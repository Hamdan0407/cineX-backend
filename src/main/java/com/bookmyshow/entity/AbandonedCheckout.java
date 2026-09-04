package com.bookmyshow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Tracks an incomplete checkout for later recovery (email in Part 2).
 * Linked 1:1 to a Booking. Does not store card data or Razorpay secrets.
 */
@Entity
@Table(
        name = "abandoned_checkouts",
        uniqueConstraints = @UniqueConstraint(name = "uk_abandoned_checkout_booking", columnNames = "booking_id")
)
@Data
public class AbandonedCheckout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "clerk_user_id", nullable = false, length = 128)
    private String clerkUserId;

    @Column(name = "show_id")
    private Long showId;

    /** Comma-separated seat numbers mirrored from booking for recovery context. */
    @Column(name = "seat_numbers", length = 512)
    private String seatNumbers;

    @Column(name = "amount")
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AbandonedCheckoutStatus status = AbandonedCheckoutStatus.CHECKOUT_STARTED;

    @Column(name = "recovery_email_sent", nullable = false)
    private boolean recoveryEmailSent = false;

    @Column(name = "recovery_email_sent_at")
    private LocalDateTime recoveryEmailSentAt;

    /** Razorpay order id only (public id) — never secrets or card PAN. */
    @Column(name = "razorpay_order_id", length = 128)
    private String razorpayOrderId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** When the checkout last became eligible for recovery (failed/cancelled). */
    @Column(name = "eligible_for_recovery_at")
    private LocalDateTime eligibleForRecoveryAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
