package com.example.healthcareappointmentmanagementsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO for carrying appointment status update requests.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentStatusRequest {

    @NotBlank(message = "Status is required")
    private String status;
}
