package com.bookmyshow.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingResponse {
    private Long bookingId;
    private String id; // e.g. "CNX-1" for frontend alias
    private String clerkUserId;
    private Long userId;
    private Long movieId;
    private Long showId;
    private Long theatreId;
    private String seatIds;
    private Double amount;
    private String bookingStatus;
    private String paymentStatus;
    private String paymentId;
    private String status;
    private Double totalAmount;
    private LocalDateTime bookingTime;
    private List<String> seatNumbers;
    private String ticketToken;
    
    // UI display helpers & frontend alias fields
    private String movieTitle;
    private String movie;     // alias for movieTitle
    private String posterPath;
    private String poster;    // alias for posterPath
    private String theatreName;
    private String theatre;   // alias for theatreName
    private String screenName;
    private String cityName;
    private String showDate;
    private String date;      // alias for showDate
    private String showTime;
    private String time;      // alias for showTime
    private List<String> seats; // alias for seatNumbers
    private Double total;     // alias for amount
}
