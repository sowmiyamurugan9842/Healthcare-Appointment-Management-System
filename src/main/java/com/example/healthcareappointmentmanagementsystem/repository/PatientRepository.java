package com.example.healthcareappointmentmanagementsystem.repository;

import com.example.healthcareappointmentmanagementsystem.entity.BloodGroup;
import com.example.healthcareappointmentmanagementsystem.entity.Gender;
import com.example.healthcareappointmentmanagementsystem.entity.Patient;
import com.example.healthcareappointmentmanagementsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Patient entity.
 * Handles database operations for Patients.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /**
     * Finds a patient profile by their base User account.
     * Used for patient dashboard profile retrieval.
     *
     * @param user base User entity
     * @return Optional containing the Patient if found
     */
    Optional<Patient> findByUser(User user);

    /**
     * Checks if a patient profile exists for a given User account.
     *
     * @param user base User entity
     * @return true if patient profile exists, false otherwise
     */
    boolean existsByUser(User user);

    /**
     * Finds all patients with a specific blood group.
     *
     * @param bloodGroup BloodGroup enum
     * @return list of Patients matching the blood group
     */
    List<Patient> findByBloodGroup(BloodGroup bloodGroup);

    /**
     * Finds all patients of a specific gender.
     *
     * @param gender Gender enum
     * @return list of Patients matching the gender
     */
    List<Patient> findByGender(Gender gender);

    /**
     * Searches for patients by address details (e.g. city), ignoring case.
     *
     * @param city address/city search keyword
     * @return list of matching Patients
     */
    List<Patient> findByAddressContainingIgnoreCase(String city);
}
