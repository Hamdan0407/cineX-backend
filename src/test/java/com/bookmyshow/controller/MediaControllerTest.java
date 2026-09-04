package com.bookmyshow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "cinex.media.cloudfront-base-url=https://d1al8zqo1izqiu.cloudfront.net",
        "cinex.media.trailer-mappings.969681=trailers/SPIDER-MAN_ BRAND NEW DAY \u2013 New Trailer (4K).mp4"
})
class MediaControllerTest {

    static final String SPIDER_MAN_TRAILER_OBJECT_KEY =
            "trailers/SPIDER-MAN_ BRAND NEW DAY \u2013 New Trailer (4K).mp4";
    static final String SPIDER_MAN_TRAILER_PLAYBACK_URL =
            "https://d1al8zqo1izqiu.cloudfront.net/trailers/SPIDER-MAN_%20BRAND%20NEW%20DAY%20%E2%80%93%20New%20Trailer%20%284K%29.mp4";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void trailerEndpointResolvesConfiguredMapping() throws Exception {
        mockMvc.perform(get("/api/media/movies/969681/trailer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.tmdbId").value(969681))
                .andExpect(jsonPath("$.trailerObjectKey").value(SPIDER_MAN_TRAILER_OBJECT_KEY))
                .andExpect(jsonPath("$.trailerPlaybackUrl")
                        .value(SPIDER_MAN_TRAILER_PLAYBACK_URL));
    }

    @Test
    void trailerEndpointReturnsUnavailableForUnknownMovie() throws Exception {
        mockMvc.perform(get("/api/media/movies/999999/trailer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }
}
