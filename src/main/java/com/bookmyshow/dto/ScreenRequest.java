package com.bookmyshow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ScreenRequest {
    @NotBlank(message = "Screen name is required")
    private String screenName;

    @NotNull(message = "Total seats is required")
    @Positive(message = "Total seats must be positive")
    private Integer totalSeats;

    @Positive(message = "Total rows must be positive")
    private Integer totalRows;

    @Positive(message = "Total columns must be positive")
    private Integer totalColumns;

    private String seatCategories;

    @NotNull(message = "Theatre ID is required")
    @Positive(message = "Theatre ID must be positive")
    private Long theatreId;
}
