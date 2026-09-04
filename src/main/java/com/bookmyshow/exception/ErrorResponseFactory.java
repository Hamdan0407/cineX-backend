package com.bookmyshow.exception;

import com.bookmyshow.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;

public final class ErrorResponseFactory {

    private ErrorResponseFactory() {
    }

    public static ErrorResponse create(HttpStatus status, String message) {
        return create(status, message, null);
    }

    public static ErrorResponse create(HttpStatus status, String message, Map<String, String> errors) {
        return ErrorResponse.builder()
                .message(message)
                .errors(errors)
                .status(status.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ResponseEntity<ErrorResponse> toResponseEntity(HttpStatus status, String message) {
        return toResponseEntity(status, message, null);
    }

    public static ResponseEntity<ErrorResponse> toResponseEntity(HttpStatus status, String message,
                                                               Map<String, String> errors) {
        return new ResponseEntity<>(create(status, message, errors), status);
    }
}
