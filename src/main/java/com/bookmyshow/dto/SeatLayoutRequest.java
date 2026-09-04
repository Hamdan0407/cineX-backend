package com.bookmyshow.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.Map;

@Data
public class SeatLayoutRequest {
    @Min(value = 1, message = "Rows must be at least 1")
    private int rows = 10;

    @Min(value = 1, message = "Columns must be at least 1")
    private int columns = 10;

    // Maps category names (VIP, Executive, Premium, Recliner, Gold, Silver) to their base price
    private Map<String, Double> categoryPrices;
    // Maps row prefix (e.g. "A", "B") to category name
    private Map<String, String> rowCategories;
}
