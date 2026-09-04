package com.bookmyshow.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TmdbNowPlayingServiceTest {

    @Test
    @DisplayName("Uses TMDB India region and en-US language constants")
    void regionAndLanguageConstants() {
        assertEquals("IN", TmdbNowPlayingService.REGION);
        assertEquals("en-US", TmdbNowPlayingService.LANGUAGE);
    }
}
