package com.bookmyshow.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class RateLimitingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Verify General API sets Rate Limit Headers and allows normal traffic")
    void testGeneralApiRateLimitHeaders() throws Exception {
        mockMvc.perform(get("/api/movies")
                        .header("X-Forwarded-For", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "120"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"));
    }

    @Test
    @DisplayName("Verify Transactional Endpoints enforce 20 req/min limit and return 429 when exceeded")
    void testTransactionalEndpointRateLimitExceeded() throws Exception {
        String testIp = "10.0.0.55";

        // Perform 20 requests to exhaust the transactional bucket for this IP
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/bookings")
                            .header("X-Forwarded-For", testIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"showId\": 1, \"seatIds\": [101]}"))
                    .andExpect(header().string("X-RateLimit-Limit", "20"));
        }

        // 21st request should be rejected with 429 Too Many Requests
        mockMvc.perform(post("/api/bookings")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showId\": 1, \"seatIds\": [101]}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value(containsString("API rate limit exceeded")));
    }

    @Test
    @DisplayName("Verify Actuator endpoints are excluded from rate limiting")
    void testActuatorEndpointExcludedFromRateLimiting() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-RateLimit-Limit"));
    }
}
