package com.bookmyshow.controller;

import com.bookmyshow.dto.SeatSelectRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SeatWebSocketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Verify REST endpoints for live seat locking and held seat retrieval")
    void testSeatLockingRestEndpoints() throws Exception {
        SeatSelectRequestDto lockRequest = SeatSelectRequestDto.builder()
                .showId(501L)
                .seatIds(List.of(99L, 100L))
                .userId("clerk-test-user")
                .action("SELECT")
                .build();

        // 1. Lock seats via REST
        mockMvc.perform(post("/api/shows/501/seats/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 2. Try to lock same seats with a different user -> should return 409 Conflict
        SeatSelectRequestDto duplicateRequest = SeatSelectRequestDto.builder()
                .showId(501L)
                .seatIds(List.of(100L))
                .userId("another-user")
                .action("SELECT")
                .build();

        mockMvc.perform(post("/api/shows/501/seats/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().string("false"));

        // 3. Get held seats for the show
        mockMvc.perform(get("/api/shows/501/seats/held"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        // 4. Release seats via REST
        mockMvc.perform(delete("/api/shows/501/seats/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockRequest)))
                .andExpect(status().isOk());
    }
}
