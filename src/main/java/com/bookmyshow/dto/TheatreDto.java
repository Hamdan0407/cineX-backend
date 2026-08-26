package com.bookmyshow.dto;

import lombok.Data;

@Data
public class TheatreDto {
    private Long id;
    private String name;
    private String city;
    private String address;
    private String amenities;
}
