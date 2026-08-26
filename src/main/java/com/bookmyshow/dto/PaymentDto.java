package com.bookmyshow.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentDto {
    private Long id;
    private Long bookingId;
    private Double amount;
    private String status;
    private String transactionId;
    private LocalDateTime createdAt;
}
