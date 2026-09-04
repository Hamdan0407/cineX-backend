package com.bookmyshow.controller;

import com.bookmyshow.entity.Show;
import com.bookmyshow.repository.ShowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RequestValidationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ShowRepository showRepository;

    @Test
    @DisplayName("Register rejects blank name and invalid email")
    void registerRejectsInvalidPayload() throws Exception {
        Map<String, Object> invalid = Map.of(
                "name", "",
                "email", "not-an-email",
                "password", "123",
                "phone", "123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.phone").exists());
    }

    @Test
    @DisplayName("Register accepts valid payload")
    void registerAcceptsValidPayload() throws Exception {
        Map<String, Object> valid = Map.of(
                "name", "Validation Test User",
                "email", "validation-user-" + System.nanoTime() + "@cinex.com",
                "password", "secret123",
                "phone", "9876543210"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(valid)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    @DisplayName("Login rejects blank email")
    void loginRejectsBlankEmail() throws Exception {
        Map<String, String> invalid = Map.of(
                "email", "",
                "password", "secret123"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.email").value("Email cannot be empty"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Booking rejects missing showId and empty seatIds")
    void bookingRejectsMissingRequiredFields() throws Exception {
        Map<String, Object> missingShowId = Map.of(
                "seatIds", List.of(1L)
        );

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingShowId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.showId").value("showId is required"));

        Map<String, Object> emptySeats = Map.of(
                "showId", 1,
                "seatIds", List.of()
        );

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptySeats)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.seatIds").value("At least one seatId is required"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Booking rejects invalid optional email format")
    void bookingRejectsInvalidEmailFormat() throws Exception {
        Map<String, Object> invalid = Map.of(
                "showId", 1,
                "seatIds", List.of(1L),
                "userEmail", "not-an-email"
        );

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.userEmail").value("Invalid email format"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Movie create rejects blank title and non-positive duration")
    void movieCreateRejectsInvalidPayload() throws Exception {
        Map<String, Object> invalid = Map.of(
                "title", "",
                "duration", -10,
                "language", "English",
                "genre", "Action",
                "releaseDate", LocalDate.now().toString()
        );

        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.title").value("Title is required"))
                .andExpect(jsonPath("$.errors.duration").value("Duration must be positive"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Theatre create rejects blank required fields")
    void theatreCreateRejectsBlankFields() throws Exception {
        Map<String, String> invalid = Map.of(
                "name", "",
                "city", "",
                "address", ""
        );

        mockMvc.perform(post("/api/theatres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Theatre name is required"))
                .andExpect(jsonPath("$.errors.city").value("City is required"))
                .andExpect(jsonPath("$.errors.address").value("Address is required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Show create rejects missing required fields")
    void showCreateRejectsMissingFields() throws Exception {
        Map<String, Object> invalid = Map.of(
                "price", -100
        );

        mockMvc.perform(post("/api/shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.movieId").value("Movie ID is required"))
                .andExpect(jsonPath("$.errors.screenId").value("Screen ID is required"))
                .andExpect(jsonPath("$.errors.showDate").value("Show date is required"))
                .andExpect(jsonPath("$.errors.showTime").value("Show time is required"))
                .andExpect(jsonPath("$.errors.price").value("Price must be positive"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Seat layout rejects non-positive rows")
    void seatLayoutRejectsInvalidDimensions() throws Exception {
        Long screenId = 1L;

        Map<String, Object> invalid = Map.of(
                "rows", 0,
                "columns", 5
        );

        mockMvc.perform(post("/api/seats/layout/{screenId}", screenId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rows").value("Rows must be at least 1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Payment verify rejects blank Razorpay fields")
    void paymentVerifyRejectsBlankFields() throws Exception {
        Map<String, Object> invalid = Map.of(
                "bookingId", 1,
                "razorpayOrderId", "",
                "razorpayPaymentId", "",
                "razorpaySignature", ""
        );

        mockMvc.perform(post("/api/payments/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.razorpayOrderId").value("Razorpay Order ID is required"))
                .andExpect(jsonPath("$.errors.razorpayPaymentId").value("Razorpay Payment ID is required"))
                .andExpect(jsonPath("$.errors.razorpaySignature").value("Razorpay Signature is required"));
    }
}
