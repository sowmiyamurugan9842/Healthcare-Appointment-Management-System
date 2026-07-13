package com.example.healthcareappointmentmanagementsystem.dto.response;

import lombok.*;

/**
 * DTO representing department information returned to the client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentResponse {

    private Long id;
    private String departmentName;
    private String description;
}
