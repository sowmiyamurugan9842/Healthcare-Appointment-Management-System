package com.example.healthcareappointmentmanagementsystem.exception;

/**
 * Custom runtime exception thrown when authentication credentials are missing, invalid,
 * or when the user fails authentication checks.
 * Mapped to HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends RuntimeException {

    /**
     * Constructs a new UnauthorizedException with the specified detail message.
     *
     * @param message detail message
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
