package com.bookmyshow.dto;

import lombok.Data;

@Data
public class ScreenDto {
    private Long id;
    private String screenName;
    private Integer totalSeats;
    private Integer totalRows;
    private Integer totalColumns;
    private String seatCategories;
    private Long theatreId;
}
