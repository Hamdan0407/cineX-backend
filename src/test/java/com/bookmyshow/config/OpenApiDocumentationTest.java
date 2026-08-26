package com.bookmyshow.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Verify OpenAPI JSON Endpoint Generates Custom Documentation")
    void testOpenApiEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("CineX Movie Ticket Booking Platform API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0-PROD"))
                .andExpect(jsonPath("$.paths['/api/movies']").exists())
                .andExpect(jsonPath("$.paths['/api/theatres']").exists())
                .andExpect(jsonPath("$.paths['/api/shows']").exists())
                .andExpect(jsonPath("$.paths['/api/bookings']").exists());
    }
}
