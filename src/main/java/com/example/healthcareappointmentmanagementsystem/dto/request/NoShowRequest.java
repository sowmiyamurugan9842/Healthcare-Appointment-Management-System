package com.example.healthcareappointmentmanagementsystem.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for providing reason when marking an appointment as NO_SHOW.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoShowRequest {

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
