package com.bookmyshow.controller;

import com.bookmyshow.entity.Movie;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Seat;
import com.bookmyshow.entity.Show;
import com.bookmyshow.entity.Theatre;
import com.bookmyshow.repository.MovieRepository;
import com.bookmyshow.repository.ScreenRepository;
import com.bookmyshow.repository.SeatRepository;
import com.bookmyshow.repository.ShowRepository;
import com.bookmyshow.repository.TheatreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlingIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ShowRepository showRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private TheatreRepository theatreRepository;
    @Autowired private ScreenRepository screenRepository;

    private Long showId;
    private Long seatId;

    @BeforeEach
    void setUp() {
        Movie movie = new Movie();
        movie.setTitle("Exception Test Movie");
        movie = movieRepository.save(movie);

        Theatre theatre = new Theatre();
        theatre.setName("Exception Test Theatre");
        theatre.setCity("Test City");
        theatre = theatreRepository.save(theatre);

        Screen screen = new Screen();
        screen.setScreenName("Exception Screen");
        screen.setTheatre(theatre);
        screen = screenRepository.save(screen);

        Seat seat = new Seat();
        seat.setSeatNumber("Z9");
        seat.setSeatType("PREMIUM");
        seat.setPrice(250.0);
        seat.setScreen(screen);
        seat = seatRepository.save(seat);
        seatId = seat.getId();

        Show show = new Show();
        show.setMovie(movie);
        show.setScreen(screen);
        show.setShowDate(LocalDate.now());
        show.setShowTime(LocalTime.of(20, 0));
        show.setPrice(250.0);
        show = showRepository.save(show);
        showId = show.getId();
    }

    @Test
    @DisplayName("GET unknown movie returns standardized 404 JSON")
    void notFoundReturnsStandardErrorResponse() throws Exception {
        mockMvc.perform(get("/api/movies/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Movie not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST booking with empty seatIds returns standardized 400 validation JSON")
    void validationErrorReturnsStandardErrorResponse() throws Exception {
        Map<String, Object> invalid = Map.of(
                "showId", showId,
                "seatIds", List.of()
        );

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.seatIds").value("At least one seatId is required"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST booking for already booked seat returns 409 conflict JSON")
    void bookingConflictReturns409() throws Exception {
        Map<String, Object> booking = Map.of(
                "showId", showId,
                "seatIds", List.of(seatId),
                "clerkUserId", "conflict-test-user"
        );

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Seat already booked")))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Non-admin movie create returns 403 forbidden JSON")
    void forbiddenReturnsStandardErrorResponse() throws Exception {
        Map<String, Object> movie = Map.of(
                "title", "Forbidden Movie",
                "duration", 120,
                "language", "English",
                "genre", "Action",
                "releaseDate", LocalDate.now().toString()
        );

        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movie)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Admin privileges")))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Admin login with invalid credentials returns 401 JSON")
    void unauthorizedAdminLoginReturns401() throws Exception {
        Map<String, String> invalid = Map.of(
                "email", "nobody@cinex.com",
                "password", "wrong-password"
        );

        mockMvc.perform(post("/api/auth/admin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid admin credentials"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Malformed JSON returns 400 without internal details")
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request body"));
    }
}
