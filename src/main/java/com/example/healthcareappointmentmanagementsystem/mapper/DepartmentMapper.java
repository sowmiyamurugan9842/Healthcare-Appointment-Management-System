package com.example.healthcareappointmentmanagementsystem.mapper;

import com.example.healthcareappointmentmanagementsystem.dto.request.DepartmentRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.DepartmentResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Department;
import org.springframework.stereotype.Component;

/**
 * Mapper class to convert between Department entity and Department DTOs.
 */
@Component
public class DepartmentMapper {

    /**
     * Converts a Department entity to a DepartmentResponse DTO.
     *
     * @param department Department entity
     * @return DepartmentResponse DTO
     */
    public DepartmentResponse toResponse(Department department) {
        if (department == null) {
            return null;
        }

        return DepartmentResponse.builder()
                .id(department.getId())
                .departmentName(department.getDepartmentName())
                .description(department.getDescription())
                .build();
    }

    /**
     * Converts a DepartmentRequest DTO to a Department entity.
     *
     * @param request DepartmentRequest DTO
     * @return Department entity
     */
    public Department toEntity(DepartmentRequest request) {
        if (request == null) {
            return null;
        }

        return Department.builder()
                .departmentName(request.getDepartmentName())
                .description(request.getDescription())
                .build();
    }
}
