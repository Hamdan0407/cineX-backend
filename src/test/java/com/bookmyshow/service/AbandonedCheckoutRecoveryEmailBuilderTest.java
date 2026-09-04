package com.bookmyshow.service;

import com.bookmyshow.dto.AbandonedCheckoutRecoveryEmailContent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbandonedCheckoutRecoveryEmailBuilderTest {

    private final AbandonedCheckoutRecoveryEmailBuilder builder = new AbandonedCheckoutRecoveryEmailBuilder();

    @Test
    void buildHtml_containsMovieTheatreSeatsAndRecoveryLink() {
        AbandonedCheckoutRecoveryEmailContent content = AbandonedCheckoutRecoveryEmailContent.builder()
                .recipientName("Hamdaan")
                .movieTitle("Dune: Part Three")
                .theatreName("PVR Sathyam")
                .cityName("Chennai")
                .showDate("12 Sep 2026")
                .showTime("8:00 PM")
                .seatNumbers("A5,A6")
                .amount("Rs. 500")
                .recoveryUrl("https://cinex.example.com/?recover=1&showId=10")
                .build();

        String html = builder.buildHtml(content);

        assertTrue(html.contains("Hello Hamdaan"));
        assertTrue(html.contains("Dune: Part Three"));
        assertTrue(html.contains("PVR Sathyam"));
        assertTrue(html.contains("Chennai"));
        assertTrue(html.contains("A5,A6"));
        assertTrue(html.contains("Rs. 500"));
        assertTrue(html.contains("https://cinex.example.com/?recover=1&amp;showId=10")
                || html.contains("https://cinex.example.com/?recover=1&showId=10"));
        assertFalse(html.contains("order_"));
        assertFalse(html.contains("razorpay"));
    }

    @Test
    void buildHtml_usesThereWhenNameMissing() {
        AbandonedCheckoutRecoveryEmailContent content = AbandonedCheckoutRecoveryEmailContent.builder()
                .recipientName("")
                .movieTitle("Movie")
                .theatreName("Theatre")
                .cityName("City")
                .showDate("Today")
                .showTime("Now")
                .seatNumbers("B1")
                .amount("Rs. 100")
                .recoveryUrl("https://cinex.example.com")
                .build();

        String html = builder.buildHtml(content);
        assertTrue(html.contains("Hello there"));
    }
}
