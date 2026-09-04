package com.bookmyshow.config;

import com.bookmyshow.exception.ErrorResponseFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes standardized {@link com.bookmyshow.dto.ErrorResponse} JSON for Spring Security
 * filter-level authentication and authorization failures.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseHandler {

    private final ObjectMapper objectMapper;

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
                write(response, HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                write(response, HttpStatus.FORBIDDEN, "Access denied");
    }

    public void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ErrorResponseFactory.create(status, message));
    }

    private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        writeError(response, status, message);
    }
}
