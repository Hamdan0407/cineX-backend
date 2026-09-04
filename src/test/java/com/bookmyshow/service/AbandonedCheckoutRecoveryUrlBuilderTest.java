package com.bookmyshow.service;

import com.bookmyshow.config.AbandonedCheckoutProperties;
import com.bookmyshow.entity.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbandonedCheckoutRecoveryUrlBuilderTest {

    private AbandonedCheckoutRecoveryUrlBuilder urlBuilder;

    @BeforeEach
    void setUp() {
        AbandonedCheckoutProperties properties = new AbandonedCheckoutProperties();
        properties.setFrontendUrl("https://cinex.example.com");
        urlBuilder = new AbandonedCheckoutRecoveryUrlBuilder(properties);
    }

    @Test
    void buildRecoveryUrl_includesShowMovieAndDisplayFields() {
        Booking booking = new Booking();
        booking.setMovieId(42L);
        booking.setMovieTitle("Interstellar");
        booking.setTheatreName("PVR Sathyam");
        booking.setShowDate("12 Sep 2026");
        booking.setShowTime("7:30 PM");

        com.bookmyshow.entity.Show show = new com.bookmyshow.entity.Show();
        show.setId(99L);
        booking.setShow(show);

        String url = urlBuilder.buildRecoveryUrl(booking);

        assertTrue(url.startsWith("https://cinex.example.com/?"));
        assertTrue(url.contains("recover=1"));
        assertTrue(url.contains("showId=99"));
        assertTrue(url.contains("movieId=42"));
        assertTrue(url.contains("movieTitle=Interstellar") || url.contains("movieTitle=Interstellar".replace(" ", "%20")) || url.contains("movieTitle=Interstellar"));
        assertTrue(url.contains("theatre="));
        assertFalse(url.contains("localhost"));
    }

    @Test
    void buildRecoveryUrl_stripsTrailingSlashFromFrontendBase() {
        AbandonedCheckoutProperties properties = new AbandonedCheckoutProperties();
        properties.setFrontendUrl("https://cinex.example.com/");
        AbandonedCheckoutRecoveryUrlBuilder builder = new AbandonedCheckoutRecoveryUrlBuilder(properties);

        String url = builder.buildRecoveryUrl(new Booking());
        assertEquals("https://cinex.example.com/?recover=1", url);
    }
}
