package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.request.DoctorRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.DoctorResponse;

import java.util.List;

/**
 * Service interface handling Doctor management operations.
 */
public interface DoctorService {

    /**
     * Creates a new Doctor profile linked to an existing User and Department.
     *
     * @param request DoctorRequest DTO containing profile details
     * @return DoctorResponse DTO
     */
    DoctorResponse createDoctor(DoctorRequest request);

    /**
     * Retrieves all registered doctors in the system.
     *
     * @return list of DoctorResponse DTOs
     */
    List<DoctorResponse> getAllDoctors();

    /**
     * Retrieves a doctor profile by their primary key ID.
     *
     * @param id doctor primary key
     * @return DoctorResponse DTO
     */
    DoctorResponse getDoctorById(Long id);

    /**
     * Retrieves all doctors belonging to a specific department.
     *
     * @param departmentId department primary key
     * @return list of DoctorResponse DTOs
     */
    List<DoctorResponse> getDoctorsByDepartment(Long departmentId);

    /**
     * Searches for doctors by specialization, ignoring case sensitivity.
     *
     * @param specialization specialization search keyword
     * @return list of matching DoctorResponse DTOs
     */
    List<DoctorResponse> searchDoctorsBySpecialization(String specialization);

    /**
     * Updates an existing doctor's professional profile and consulting hours.
     *
     * @param id      doctor primary key
     * @param request DoctorRequest DTO containing updated parameters
     * @return updated DoctorResponse DTO
     */
    DoctorResponse updateDoctor(Long id, DoctorRequest request);

    /**
     * Deletes a doctor's profile and their associated User account from the database.
     *
     * @param id doctor primary key
     */
    void deleteDoctor(Long id);
}
