package com.bookmyshow.dto;

import lombok.Data;

@Data
public class SeatResponse {
    private Long id;
    private String seatNumber;
    private String seatType;
    private Double price;
    private Long screenId;
}
