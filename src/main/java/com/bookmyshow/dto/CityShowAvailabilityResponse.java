package com.bookmyshow.dto;

import lombok.Data;

import java.util.List;

@Data
public class CityShowAvailabilityResponse {
    private String city;
    private List<Long> tmdbIds;
    private List<String> languages;
}
