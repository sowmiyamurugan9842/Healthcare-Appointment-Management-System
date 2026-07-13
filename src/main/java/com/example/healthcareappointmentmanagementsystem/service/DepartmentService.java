package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.request.DepartmentRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.DepartmentResponse;

import java.util.List;

/**
 * Service interface handling hospital department management operations.
 */
public interface DepartmentService {

    /**
     * Creates a new medical department in the system.
     *
     * @param request DepartmentRequest DTO
     * @return DepartmentResponse DTO
     */
    DepartmentResponse createDepartment(DepartmentRequest request);

    /**
     * Retrieves all registered departments in the hospital.
     * Used for list screens and dropdown options.
     *
     * @return list of DepartmentResponse DTOs
     */
    List<DepartmentResponse> getAllDepartments();

    /**
     * Retrieves a specific department by its primary key ID.
     *
     * @param id department primary key
     * @return DepartmentResponse DTO
     */
    DepartmentResponse getDepartmentById(Long id);

    /**
     * Updates an existing department's name and description.
     *
     * @param id      department primary key
     * @param request DepartmentRequest DTO containing updated info
     * @return updated DepartmentResponse DTO
     */
    DepartmentResponse updateDepartment(Long id, DepartmentRequest request);

    /**
     * Deletes a department from the database by its primary key.
     *
     * @param id department primary key
     */
    void deleteDepartment(Long id);
}
