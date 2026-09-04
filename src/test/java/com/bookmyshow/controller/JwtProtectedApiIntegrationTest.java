package com.bookmyshow.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwtProtectedApiIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private String userToken() {
        return JWT.create()
                .withSubject("clerk-user-phase6")
                .withIssuer("https://test.clerk.dev")
                .withClaim("role", "USER")
                .withExpiresAt(new Date(System.currentTimeMillis() + 120_000))
                .sign(Algorithm.HMAC256("test-secret"));
    }

    private String adminToken() {
        return JWT.create()
                .withSubject("clerk-admin-phase6")
                .withIssuer("https://test.clerk.dev")
                .withClaim("role", "ADMIN")
                .withExpiresAt(new Date(System.currentTimeMillis() + 120_000))
                .sign(Algorithm.HMAC256("test-secret"));
    }

    @Test
    @DisplayName("Valid USER JWT authenticates protected clerk booking history endpoint")
    void validUserJwtAuthenticatesProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/bookings/clerk/{clerkUserId}", "clerk-user-phase6")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Valid ADMIN JWT grants access to admin cache endpoint")
    void validAdminJwtGrantsAdminAccess() throws Exception {
        mockMvc.perform(get("/api/cache/stats")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Valid USER JWT cannot access admin cache endpoint")
    void validUserJwtCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/cache/stats")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Missing Authorization header returns standardized 401")
    void missingAuthorizationHeaderReturns401() throws Exception {
        mockMvc.perform(get("/api/bookings/clerk/{clerkUserId}", "clerk-user-phase6"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.status").value(401));
    }
}
