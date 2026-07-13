package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.request.PatientRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.PatientResponse;
import com.example.healthcareappointmentmanagementsystem.entity.BloodGroup;
import com.example.healthcareappointmentmanagementsystem.entity.Gender;

import java.util.List;

/**
 * Service interface handling Patient profile management operations.
 */
public interface PatientService {

    /**
     * Creates a new Patient profile linked to an existing User account.
     *
     * @param request PatientRequest DTO containing profile details
     * @return PatientResponse DTO
     */
    PatientResponse createPatient(PatientRequest request);

    /**
     * Retrieves all registered patients in the system.
     *
     * @return list of PatientResponse DTOs
     */
    List<PatientResponse> getAllPatients();

    /**
     * Retrieves a patient profile by their primary key ID.
     *
     * @param id patient primary key
     * @return PatientResponse DTO
     */
    PatientResponse getPatientById(Long id);

    /**
     * Retrieves patients filtered by their blood group.
     * Useful for clinical queries and emergency lists.
     *
     * @param bloodGroup BloodGroup enum
     * @return list of PatientResponse DTOs
     */
    List<PatientResponse> getPatientsByBloodGroup(BloodGroup bloodGroup);

    /**
     * Retrieves patients filtered by their gender.
     *
     * @param gender Gender enum
     * @return list of PatientResponse DTOs
     */
    List<PatientResponse> getPatientsByGender(Gender gender);

    /**
     * Updates an existing patient's clinical info and contact details.
     *
     * @param id      patient primary key
     * @param request PatientRequest DTO containing updated parameters
     * @return updated PatientResponse DTO
     */
    PatientResponse updatePatient(Long id, PatientRequest request);

    /**
     * Deletes a patient profile and their associated User account from the database.
     *
     * @param id patient primary key
     */
    void deletePatient(Long id);
}
