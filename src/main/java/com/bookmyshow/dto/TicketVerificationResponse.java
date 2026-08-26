package com.bookmyshow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketVerificationResponse {
    private String verificationStatus; // "VALID" or "INVALID TICKET"
    private String movie;
    private String theatre;
    private String showDate;
    private String showTime;
    private String seats;
    private String bookingStatus;
    private String customerName;
    private Long bookingId;
    private String ticketToken;
    private Double amount;
    private String city;

    public static TicketVerificationResponse invalid() {
        TicketVerificationResponse resp = new TicketVerificationResponse();
        resp.setVerificationStatus("INVALID TICKET");
        return resp;
    }
}
