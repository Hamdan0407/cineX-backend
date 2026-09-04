package com.bookmyshow.exception;

import com.bookmyshow.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Business validation exception returns 400")
    void businessValidationReturnsBadRequest() {
        ResponseEntity<ErrorResponse> response =
                handler.handleValidationException(new ValidationException("showId is required"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("showId is required", response.getBody().getMessage());
        assertNull(response.getBody().getErrors());
    }

    @Test
    @DisplayName("Resource not found returns 404")
    void resourceNotFoundReturnsNotFound() {
        ResponseEntity<ErrorResponse> response =
                handler.handleResourceNotFoundException(new ResourceNotFoundException("Movie not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Movie not found", response.getBody().getMessage());
        assertNull(response.getBody().getErrors());
    }

    @Test
    @DisplayName("Seat already booked returns 409")
    void seatAlreadyBookedReturnsConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleSeatAlreadyBookedException(
                new SeatAlreadyBookedException("Seat already booked for show 1"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Seat already booked"));
    }

    @Test
    @DisplayName("Unavailable distributed seat lock returns a safe 503")
    void seatLockUnavailableReturnsServiceUnavailable() {
        ResponseEntity<ErrorResponse> response = handler.handleSeatLockUnavailable(
                new SeatLockUnavailableException("Seat locking is temporarily unavailable. Please try again shortly.", new RuntimeException()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Seat locking is temporarily unavailable. Please try again shortly.", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Unauthorized exception returns 401")
    void unauthorizedReturns401() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnauthorizedException(new UnauthorizedException("Invalid admin credentials"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid admin credentials", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Security access denied returns 403")
    void securityAccessDeniedReturnsForbidden() {
        ResponseEntity<ErrorResponse> response = handler.handleSecurityException(
                new SecurityException("Access Denied: Admin privileges required"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("Security not authenticated returns 401")
    void securityNotAuthenticatedReturnsUnauthorized() {
        ResponseEntity<ErrorResponse> response = handler.handleSecurityException(
                new SecurityException("Not authenticated"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("ResponseStatusException returns matching ErrorResponse status")
    void responseStatusExceptionUsesErrorResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to fetch movies from TMDB right now"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Unable to fetch movies from TMDB right now", response.getBody().getMessage());
        assertEquals(503, response.getBody().getStatus());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("RestClientException returns 502 without external details")
    void restClientExceptionReturnsBadGateway() {
        ResponseEntity<ErrorResponse> response = handler.handleRestClientException(
                new RestClientException("Connection reset by peer"));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("External service is temporarily unavailable", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("Connection reset"));
    }

    @Test
    @DisplayName("Generic exception returns 500 without internal details")
    void genericExceptionHidesInternalDetails() {
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(
                new RuntimeException("SQL syntax error near 'password'"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred. Please try again later.", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("SQL"));
        assertFalse(response.getBody().getMessage().contains("password"));
        assertNotNull(response.getBody().getTimestamp());
    }
}
