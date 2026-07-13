package com.example.healthcareappointmentmanagementsystem.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Standardized structure for error responses returned by the API.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
