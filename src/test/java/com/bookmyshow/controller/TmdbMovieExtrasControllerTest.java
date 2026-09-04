package com.bookmyshow.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TmdbMovieExtrasControllerTest {

    @DynamicPropertySource
    static void forceTestTmdbKey(DynamicPropertyRegistry registry) {
        registry.add("tmdb.api.key", () -> "test-key");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Credits endpoint returns an empty payload when TMDB is unavailable in tests")
    void creditsReturnsEmptyArraysWithoutTmdbKey() throws Exception {
        mockMvc.perform(get("/api/tmdb/movie/653346/credits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cast").isArray())
                .andExpect(jsonPath("$.crew").isArray());
    }

    @Test
    @DisplayName("Similar endpoint returns an empty results envelope when TMDB is unavailable in tests")
    void similarReturnsEmptyResultsWithoutTmdbKey() throws Exception {
        mockMvc.perform(get("/api/tmdb/movie/653346/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results.length()").value(0));
    }
}
