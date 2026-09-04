package com.bookmyshow.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ShowResponse {
    private Long id;
    private Long movieId;
    private Long screenId;
    private Long theatreId;
    private String theatreName;
    private String city;
    private String screenName;
    private String screeningLanguage;
    private String movieTitle;
    private LocalTime showTime;
    private LocalDate showDate;
    private Double price;
    private Integer availableSeats;
}
