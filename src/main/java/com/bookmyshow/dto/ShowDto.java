package com.bookmyshow.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ShowDto {
    private Long id;
    private Long movieId;
    private Long screenId;
    private LocalTime showTime;
    private LocalDate showDate;
    private Double price;
    private Integer availableSeats;
}
