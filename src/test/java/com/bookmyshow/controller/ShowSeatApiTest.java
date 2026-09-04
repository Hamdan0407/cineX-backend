package com.bookmyshow.controller;

import com.bookmyshow.entity.Show;
import com.bookmyshow.support.ShowInventoryTestSupport;
import org.junit.jupiter.api.BeforeEach;
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
class ShowSeatApiTest extends ShowInventoryTestSupport {

    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void seedInventory() {
        ensureSampleShowInventory();
    }

    @Test
    void showApiReturnsShowsForMovie() throws Exception {
        Show show = requireSampleShow();

        mockMvc.perform(get("/api/shows/movie/{movieId}", show.getMovie().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(show.getId()))
                .andExpect(jsonPath("$[0].movieId").value(show.getMovie().getId()))
                .andExpect(jsonPath("$[0].screenId").value(show.getScreen().getId()));
    }

    @Test
    void seatApiReturnsSeatsForShowScreen() throws Exception {
        Show show = requireSampleShow();

        mockMvc.perform(get("/api/shows/{showId}/seats", show.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seatId").isNumber())
                .andExpect(jsonPath("$[0].seatNumber").isString())
                .andExpect(jsonPath("$[0].rowIndex").isNumber())
                .andExpect(jsonPath("$[0].columnIndex").isNumber())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(20)))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }
}
