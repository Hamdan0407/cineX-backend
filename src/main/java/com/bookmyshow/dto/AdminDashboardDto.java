package com.bookmyshow.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AdminDashboardDto {
    private Double todayRevenue;
    private Double monthlyRevenue;
    private Long totalBookings;
    private Long moviesRunning;
    private Long showsRunning;
    private Double occupancyPercentage;
    private List<String> popularMovies;
    private List<String> popularTheatres;
    private Map<String, Double> revenueGraphData;
    private Map<String, Long> bookingGraphData;
}
