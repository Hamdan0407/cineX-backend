package com.bookmyshow.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.bookmyshow.entity.Booking;
import com.bookmyshow.entity.Show;
import com.bookmyshow.repository.BookingRepository;
import com.bookmyshow.repository.SeatRepository;
import com.bookmyshow.repository.ShowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bookmyshow.support.ShowInventoryTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationTest extends ShowInventoryTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private SeatRepository seatRepository;

    private Long ownedBookingId;
    private Long foreignBookingId;

    @BeforeEach
    void setUp() {
        ensureSampleShowInventory();
        Show show = requireSampleShow();

        Booking owned = new Booking();
        owned.setShow(show);
        owned.setClerkUserId("user");
        owned.setBookingStatus("BOOKED");
        owned.setPaymentStatus("SUCCESS");
        owned.setSeatIds("A1");
        owned.setAmount(250.0);
        owned = bookingRepository.save(owned);
        ownedBookingId = owned.getId();

        Booking foreign = new Booking();
        foreign.setShow(show);
        foreign.setClerkUserId("other-user");
        foreign.setBookingStatus("BOOKED");
        foreign.setPaymentStatus("SUCCESS");
        foreign.setSeatIds("B1");
        foreign.setAmount(250.0);
        foreign = bookingRepository.save(foreign);
        foreignBookingId = foreign.getId();
    }

    private static ResultMatcher[] standardErrorContract(int expectedStatus) {
        return new ResultMatcher[]{
                jsonPath("$.message").exists(),
                jsonPath("$.status").value(expectedStatus),
                jsonPath("$.timestamp").exists(),
                jsonPath("$.stackTrace").doesNotExist(),
                jsonPath("$.password").doesNotExist()
        };
    }

    @Test
    @DisplayName("Public movie browsing works without authentication")
    void publicEndpointAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("City search works without authentication")
    void citySearchAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/cities/search").param("query", "coimbatore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Coimbatore"))
                .andExpect(jsonPath("$[0].state").value("Tamil Nadu"));
    }

    @Test
    @DisplayName("City catalog works without authentication")
    void cityCatalogAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].normalizedName").exists());
    }

    @Test
    @DisplayName("City search works with an invalid Bearer token (public endpoint)")
    void citySearchIgnoresInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/cities/search").param("query", "chennai")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Chennai"));
    }

    /**
     * Regression test: /api/cities was permitAll in SecurityConfig but missing from
     * ClerkJwtAuthenticationFilter.shouldNotFilter(), so a stale Clerk token turned the public
     * city catalog into a 401 while /api/cities/search kept working.
     */
    @Test
    @DisplayName("City catalog works with an invalid Bearer token (public endpoint)")
    void cityCatalogIgnoresInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/cities")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[?(@.name == 'Ambur')]").exists());
    }

    @Test
    @DisplayName("Protected booking endpoint rejects unauthenticated request with 401 ErrorResponse")
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/bookings/{id}", ownedBookingId))
                .andExpect(status().isUnauthorized())
                .andExpectAll(standardErrorContract(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("Wallet endpoints reject unauthenticated requests")
    void walletRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/wallet"))
                .andExpect(status().isUnauthorized())
                .andExpectAll(standardErrorContract(401));
    }

    @Test
    @DisplayName("Coupon redemption rejects unauthenticated requests")
    void couponRedemptionRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/coupons/redeem").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"CINEX300\"}"))
                .andExpect(status().isUnauthorized())
                .andExpectAll(standardErrorContract(401));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("USER cannot access admin cache endpoint")
    void userCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(status().isForbidden())
                .andExpectAll(standardErrorContract(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN can access admin cache endpoint")
    void adminCanAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Malformed JWT is rejected with standardized 401 ErrorResponse")
    void invalidJwtIsRejected() throws Exception {
        mockMvc.perform(get("/api/bookings/{id}", ownedBookingId)
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpectAll(standardErrorContract(401))
                .andExpect(jsonPath("$.message", not(containsString("secret"))));
    }

    @Test
    @DisplayName("Expired JWT is rejected with standardized 401 ErrorResponse")
    void expiredJwtIsRejected() throws Exception {
        String expiredToken = JWT.create()
                .withSubject("user_test")
                .withIssuer("https://test.clerk.dev")
                .withExpiresAt(new Date(System.currentTimeMillis() - 60_000))
                .sign(Algorithm.HMAC256("test-secret"));

        mockMvc.perform(get("/api/bookings/{id}", ownedBookingId)
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpectAll(standardErrorContract(401))
                .andExpect(jsonPath("$.message").value("Expired JWT token"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("Booking creation uses authenticated identity instead of request-supplied clerkUserId")
    void bookingCreationUsesJwtIdentity() throws Exception {
        Show show = showRepository.findAll().get(0);
        Long seatId = seatRepository.findByScreenId(show.getScreen().getId()).get(0).getId();
        Map<String, Object> booking = Map.of(
                "showId", show.getId(),
                "seatIds", List.of(seatId),
                "clerkUserId", "spoofed-attacker-id"
        );

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clerkUserId").value("user"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("User cannot access another user's booking")
    void userCannotAccessForeignBooking() throws Exception {
        mockMvc.perform(get("/api/bookings/{id}", foreignBookingId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("User can access their own booking")
    void userCanAccessOwnBooking() throws Exception {
        mockMvc.perform(get("/api/bookings/{id}", ownedBookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(ownedBookingId))
                .andExpect(jsonPath("$.clerkUserId").value("user"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("User cannot list another user's clerk booking history")
    void userCannotAccessForeignClerkBookings() throws Exception {
        mockMvc.perform(get("/api/bookings/clerk/{clerkUserId}", "other-user"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Security errors do not leak sensitive details")
    void securityErrorsDoNotLeakSensitiveData() throws Exception {
        mockMvc.perform(get("/api/bookings/{id}", foreignBookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.signature").doesNotExist());
    }
}
