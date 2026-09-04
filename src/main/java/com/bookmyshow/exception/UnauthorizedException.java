package com.bookmyshow.exception;

/**
 * Thrown when authentication fails (invalid credentials, missing token, etc.).
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
