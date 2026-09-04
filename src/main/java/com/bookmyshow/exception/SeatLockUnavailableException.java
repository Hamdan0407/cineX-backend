package com.bookmyshow.exception;

/** Raised when the required distributed Redis seat-lock store cannot be reached. */
public class SeatLockUnavailableException extends RuntimeException {
    public SeatLockUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
