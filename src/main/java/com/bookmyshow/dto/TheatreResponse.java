package com.bookmyshow.dto;

import lombok.Data;

@Data
public class TheatreResponse {
    private Long id;
    private String name;
    private String city;
    private String address;
    private String amenities;
}
