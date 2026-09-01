package com.bookmyshow.dto;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class BookingRequest {
    private Long userId;        // Optional when using clerkUserId
    private String clerkUserId;
    private String userEmail;   // Optional customer email
    
    @NotNull(message = "showId is required")
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
