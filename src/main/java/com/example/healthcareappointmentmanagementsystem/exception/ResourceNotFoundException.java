package com.example.healthcareappointmentmanagementsystem.exception;

/**
 * Custom runtime exception thrown when a requested resource is not found in the database.
 * Mapped to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     *
     * @param message detail message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
