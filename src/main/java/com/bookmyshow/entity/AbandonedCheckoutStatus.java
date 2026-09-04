package com.bookmyshow.entity;

/**
 * Lifecycle of an abandoned-checkout recovery record.
 * Recovery email sending is Part 2 — RECOVERY_SENT is reserved for that.
 */
public enum AbandonedCheckoutStatus {
    /** Booking created; user entered checkout. */
    CHECKOUT_STARTED,
    /** Razorpay order created; payment modal may be open. */
    PAYMENT_PENDING,
    /** Signature verification failed or gateway reported failure. */
    PAYMENT_FAILED,
    /** User dismissed/cancelled Razorpay or timed out. Still recoverable. */
    PAYMENT_CANCELLED,
    /** Payment verified and booking completed. Not eligible for recovery. */
    COMPLETED,
    /** Part 2: recovery email has been sent. */
    RECOVERY_SENT
}
