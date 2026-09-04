package com.bookmyshow.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Data
public class BookingRequest {
    private Long userId;        // Optional when using clerkUserId
    private String clerkUserId;
    @Email(message = "Invalid email format")
    private String userEmail;   // Optional customer email
    
    @NotNull(message = "showId is required")
    @Positive(message = "showId must be positive")
    private Long showId;
    private Long movieId;
    private Long theatreId;
    
    @NotEmpty(message = "At least one seatId is required")
    private List<@NotNull(message = "seatId cannot be null") Long> seatIds;
    
    private Double amount;
    
    // UI display helpers for TMDB dynamic shows
    private String movieTitle;
    private String posterPath;
    private String theatreName;
    private String cityName;
    private String showDate;
    private String showTime;
}
