package com.example.healthcareappointmentmanagementsystem.repository;

import com.example.healthcareappointmentmanagementsystem.entity.Department;
import com.example.healthcareappointmentmanagementsystem.entity.Doctor;
import com.example.healthcareappointmentmanagementsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Doctor entity.
 * Handles database operations for Doctors.
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /**
     * Finds all doctors belonging to a specific department.
     *
     * @param department Department entity
     * @return list of Doctors in the department
     */
    List<Doctor> findByDepartment(Department department);

    /**
     * Searches for doctors by specialization keyword, ignoring case.
     *
     * @param specialization specialization search keyword
     * @return list of Doctors matching the specialization keyword
     */
    List<Doctor> findBySpecializationContainingIgnoreCase(String specialization);

    /**
     * Finds doctors with experience years greater than or equal to a target.
     *
     * @param years minimum experience years
     * @return list of matching Doctors
     */
    List<Doctor> findByExperienceYearsGreaterThanEqual(Integer years);

    /**
     * Finds doctors with a consultation fee less than or equal to a target.
     *
     * @param fee maximum consultation fee
     * @return list of matching Doctors
     */
    List<Doctor> findByConsultationFeeLessThanEqual(Double fee);

    /**
     * Finds a doctor profile by their base User account.
     *
     * @param user base User entity
     * @return Optional containing the Doctor if found
     */
    Optional<Doctor> findByUser(User user);

    /**
     * Checks if a doctor profile exists for a given User account.
     *
     * @param user base User entity
     * @return true if doctor profile exists, false otherwise
     */
    boolean existsByUser(User user);
}
