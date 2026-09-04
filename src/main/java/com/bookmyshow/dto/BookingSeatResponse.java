package com.bookmyshow.dto;

import lombok.Data;

@Data
public class BookingSeatResponse {
    private Long id;
    private Long bookingId;
    private Long seatId;
    private String seatNumber;
    private String status;
}
