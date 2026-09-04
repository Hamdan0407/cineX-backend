package com.bookmyshow.dto;

import lombok.Data;

@Data
public class ShowSeatDto {
    private Long seatId;
    private String seatNumber;
    private String seatType;
    private String status;
    private Double price;
    private String rowLabel;
    private Integer rowIndex;
    private Integer columnIndex;
    private Boolean wheelchairAccessible;
}
