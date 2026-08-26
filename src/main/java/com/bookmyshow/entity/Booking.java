package com.bookmyshow.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking")
@Data
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // bookingId

    @Column(name = "clerk_user_id")
    private String clerkUserId;

    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "theatre_id")
    private Long theatreId;

    @Column(name = "seat_ids")
    private String seatIds; // Comma-separated seat IDs or seat names like "A1,A2"

    private Double amount;
    private String bookingStatus; // PENDING_PAYMENT, BOOKED, FAILED, CANCELLED
    private String paymentStatus; // PENDING, SUCCESS, FAILED
    private String paymentId;     // Razorpay payment_id
    private String orderId;       // Razorpay order_id
    private String currency = "INR";
    private String signature;     // Razorpay signature upon verification

    @Column(name = "ticket_token", unique = true)
    private String ticketToken;   // Secure UUID token for digital ticket verification

    @Column(name = "user_email")
    private String userEmail;     // Customer email for HTML ticket delivery

    // Helper UI display fields for dynamic TMDB movie bookings
    private String movieTitle;
    private String posterPath;
    private String theatreName;
    private String cityName;
    private String showDate;
    private String showTime;

    // Backward compatibility with existing Phase 1 schema and tests
    @ManyToOne(optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @ManyToOne(optional = true)
    @JoinColumn(name = "show_id", nullable = true)
    private Show show;

    private String status;
    private Double totalAmount;
    private LocalDateTime bookingTime = LocalDateTime.now();
}
