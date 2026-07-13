package com.example.healthcareappointmentmanagementsystem.exception;

/**
 * Custom runtime exception thrown when a resource creation fails because the resource
 * (or a unique key of the resource) already exists in the system.
 * Mapped to HTTP 409 Conflict.
 */
public class DuplicateResourceException extends RuntimeException {

    /**
     * Constructs a new DuplicateResourceException with the specified detail message.
     *
     * @param message detail message
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}
