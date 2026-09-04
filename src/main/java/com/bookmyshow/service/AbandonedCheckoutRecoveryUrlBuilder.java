package com.bookmyshow.service;

import com.bookmyshow.config.AbandonedCheckoutProperties;
import com.bookmyshow.entity.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AbandonedCheckoutRecoveryUrlBuilder {

    private final AbandonedCheckoutProperties properties;

    public String buildRecoveryUrl(Booking booking) {
        if (booking == null) {
            return normalizeBaseUrl(properties.getFrontendUrl());
        }

        Long showId = booking.getShow() != null ? booking.getShow().getId() : null;
        Long movieId = booking.getMovieId();

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(normalizeBaseUrl(properties.getFrontendUrl()) + "/")
                .queryParam("recover", "1");

        if (showId != null) {
            builder.queryParam("showId", showId);
        }
        if (movieId != null) {
            builder.queryParam("movieId", movieId);
        }
        if (booking.getMovieTitle() != null && !booking.getMovieTitle().isBlank()) {
            builder.queryParam("movieTitle", encode(booking.getMovieTitle()));
        }
        if (booking.getTheatreName() != null && !booking.getTheatreName().isBlank()) {
            builder.queryParam("theatre", encode(booking.getTheatreName()));
        }
        if (booking.getShowDate() != null && !booking.getShowDate().isBlank()) {
            builder.queryParam("showDate", encode(booking.getShowDate()));
        }
        if (booking.getShowTime() != null && !booking.getShowTime().isBlank()) {
            builder.queryParam("showTime", encode(booking.getShowTime()));
        }

        return builder.build(true).toUriString();
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:5173";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
