package com.bookmyshow.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentResponse {
    private String status;
    private String transactionId;
    private String orderId;
    private Long bookingId;
    private String message;
    private String ticketToken;

    public PaymentResponse(String status, String transactionId) {
        this.status = status;
        this.transactionId = transactionId;
    }

    public PaymentResponse(String status, String transactionId, String orderId, Long bookingId, String message) {
        this.status = status;
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.bookingId = bookingId;
        this.message = message;
    }
}
