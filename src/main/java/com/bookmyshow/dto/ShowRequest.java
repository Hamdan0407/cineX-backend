package com.bookmyshow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ShowRequest {
    @NotNull(message = "Movie ID is required")
    @Positive(message = "Movie ID must be positive")
    private Long movieId;

    @NotNull(message = "Screen ID is required")
    @Positive(message = "Screen ID must be positive")
    private Long screenId;

    @NotNull(message = "Show time is required")
    private LocalTime showTime;

    @NotNull(message = "Show date is required")
    private LocalDate showDate;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private Double price;
}
