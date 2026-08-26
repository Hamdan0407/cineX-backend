package com.bookmyshow.dto;

import lombok.Data;
import java.util.List;

@Data
public class BookingRequest {
    private Long userId;        // Optional when using clerkUserId
    private String clerkUserId;
    private String userEmail;   // Optional customer email
    
    private Long showId;        // Optional when booking dynamic TMDB movie shows
    private Long movieId;
    private Long theatreId;
    
    private List<Long> seatIds; // Optional when using seatNames
    private List<String> seatNames; // e.g., ["A1", "A2"]
    
    private Double amount;
    
    // UI display helpers for TMDB dynamic shows
    private String movieTitle;
    private String posterPath;
    private String theatreName;
    private String cityName;
    private String showDate;
    private String showTime;
}
