package com.bookmyshow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiErrorConsistencyTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static ResultMatcher[] standardErrorContract(int expectedStatus) {
        return new ResultMatcher[]{
                jsonPath("$.message").exists(),
                jsonPath("$.status").value(expectedStatus),
                jsonPath("$.timestamp").exists(),
                jsonPath("$.stackTrace").doesNotExist(),
                jsonPath("$.trace").doesNotExist(),
                jsonPath("$.password").doesNotExist()
        };
    }

    @Test
    @DisplayName("404 responses follow ErrorResponse contract")
    void notFoundUsesStandardErrorContract() throws Exception {
        mockMvc.perform(get("/api/theatres/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpectAll(standardErrorContract(404))
                .andExpect(jsonPath("$.message").value("Theatre not found"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("400 validation responses include field errors and standard contract")
    void validationUsesStandardErrorContract() throws Exception {
        Map<String, String> invalid = Map.of("email", "", "password", "secret");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpectAll(standardErrorContract(400))
                .andExpect(jsonPath("$.message").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("Invalid path variable type returns 400 ErrorResponse")
    void invalidPathVariableReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/movies/{id}", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpectAll(standardErrorContract(400))
                .andExpect(jsonPath("$.message", containsString("not-a-number")));
    }

    @Test
    @DisplayName("Unauthenticated protected booking request returns standardized security error")
    void unauthenticatedProtectedRequestReturnsStandardError() throws Exception {
        Map<String, Object> booking = Map.of(
                "showId", 1,
                "seatIds", java.util.List.of(1L)
        );

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isUnauthorized())
                .andExpectAll(standardErrorContract(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Authenticated non-admin cache access returns 403 ErrorResponse")
    void authenticatedForbiddenRequestReturnsStandardError() throws Exception {
        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(status().isForbidden())
                .andExpectAll(standardErrorContract(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin delete missing movie returns 404 ErrorResponse")
    void adminDeleteMissingResourceReturnsNotFound() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/movies/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpectAll(standardErrorContract(404))
                .andExpect(jsonPath("$.message").value("Movie not found"));
    }

    @Test
    @DisplayName("Malformed JSON never leaks internal parser details")
    void malformedJsonDoesNotExposeInternalDetails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpectAll(standardErrorContract(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request body"))
                .andExpect(jsonPath("$.message", not(containsString("Unrecognized token"))));
    }
}
