package com.example.healthcareappointmentmanagementsystem.exception;

/**
 * Custom runtime exception thrown when the client sends an invalid request, violates business rules,
 * or provides incompatible arguments.
 * Mapped to HTTP 400 Bad Request.
 */
public class BadRequestException extends RuntimeException {

    /**
     * Constructs a new BadRequestException with the specified detail message.
     *
     * @param message detail message
     */
    public BadRequestException(String message) {
        super(message);
    }
}
