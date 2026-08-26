package com.bookmyshow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String orderId;
    private Long bookingId;
    private Long amountInPaise;
    private Double amount;
    private String currency;
    private String keyId;
    private String name;
    private String description;
    private String movieTitle;
    private String userEmail;
}
