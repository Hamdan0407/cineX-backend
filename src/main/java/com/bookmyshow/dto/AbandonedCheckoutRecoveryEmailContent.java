package com.bookmyshow.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AbandonedCheckoutRecoveryEmailContent {
    String recipientName;
    String movieTitle;
    String theatreName;
    String cityName;
    String showDate;
    String showTime;
    String seatNumbers;
    String amount;
    String recoveryUrl;
}
