package com.bookmyshow.dto;

import lombok.Data;
import java.util.Map;

@Data
public class SeatLayoutRequest {
    private int rows = 10;
    private int columns = 10;
    // Maps category names (VIP, Executive, Premium, Recliner, Gold, Silver) to their base price
    private Map<String, Double> categoryPrices;
    // Maps row prefix (e.g. "A", "B") to category name
    private Map<String, String> rowCategories;
}
