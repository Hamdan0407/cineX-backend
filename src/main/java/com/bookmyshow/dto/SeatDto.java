package com.bookmyshow.dto;

import lombok.Data;

@Data
public class SeatDto {
    private Long id;
    private String seatNumber;
    private String seatType;
    private Double price;
    private Long screenId;
}
