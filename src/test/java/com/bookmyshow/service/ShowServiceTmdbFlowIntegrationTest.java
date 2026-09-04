package com.bookmyshow.service;

import com.bookmyshow.support.ShowInventoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShowServiceTmdbFlowIntegrationTest extends ShowInventoryTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Show lookup by TMDB id provisions inventory for Chennai")
    void showsByTmdbIdReturnsCityShowtimes() throws Exception {
        mockMvc.perform(get("/api/shows/tmdb/{tmdbId}", SAMPLE_TMDB_ID).param("city", SAMPLE_CITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].city").value("Chennai"))
                .andExpect(jsonPath("$[0].showDate").exists())
                .andExpect(jsonPath("$[0].showTime").exists());
    }

    @Test
    @DisplayName("On-demand TMDB sync provisions Mumbai showtimes for a catalog movie id")
    void onDemandSyncCreatesShowsForNewTmdbMovie() throws Exception {
        long jawanTmdbId = 677179L;

        mockMvc.perform(get("/api/shows/tmdb/{tmdbId}", jawanTmdbId).param("city", "Mumbai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].city").value("Mumbai"));
    }

    @Test
    @DisplayName("Unknown TMDB id returns an empty show list rather than fake inventory")
    void unknownTmdbIdReturnsEmptyShows() throws Exception {
        mockMvc.perform(get("/api/shows/tmdb/{tmdbId}", 1L).param("city", "Chennai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Show lookup keeps city context and does not default to another city")
    void showLookupDoesNotLeakAnotherCity() throws Exception {
        mockMvc.perform(get("/api/shows/tmdb/{tmdbId}", SAMPLE_TMDB_ID).param("city", "Hyderabad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Hyderabad"))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].screeningLanguage").exists());
    }
}
