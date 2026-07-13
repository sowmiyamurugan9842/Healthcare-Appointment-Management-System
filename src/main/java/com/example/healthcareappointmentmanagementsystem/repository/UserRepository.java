package com.example.healthcareappointmentmanagementsystem.repository;

import com.example.healthcareappointmentmanagementsystem.entity.Role;
import com.example.healthcareappointmentmanagementsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity.
 * Handles database operations for User accounts.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address.
     * Used primarily for authentication/login.
     *
     * @param email user email
     * @return Optional containing the User if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists with the given email.
     * Used during registration to prevent duplicate accounts.
     *
     * @param email user email
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Finds all users with a specific role (ADMIN, DOCTOR, PATIENT).
     * Useful for listing all doctors or patients.
     *
     * @param role user role enum
     * @return list of Users matching the role
     */
    List<User> findByRole(Role role);

    /**
     * Finds all users based on their active status.
     *
     * @param enabled account status (true/false)
     * @return list of Users matching the status
     */
    List<User> findByEnabled(Boolean enabled);
}
