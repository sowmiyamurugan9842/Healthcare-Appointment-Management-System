package com.example.healthcareappointmentmanagementsystem.repository;

import com.example.healthcareappointmentmanagementsystem.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Department entity.
 * Handles database operations for hospital departments.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * Finds a department by its exact name.
     *
     * @param departmentName exact department name
     * @return Optional containing the Department if found
     */
    Optional<Department> findByDepartmentName(String departmentName);

    /**
     * Checks if a department exists with the given name.
     *
     * @param departmentName department name
     * @return true if exists, false otherwise
     */
    boolean existsByDepartmentName(String departmentName);

    /**
     * Searches for departments whose name contains a search keyword, ignoring letter case.
     *
     * @param keyword search keyword
     * @return list of Departments containing the keyword
     */
    List<Department> findByDepartmentNameContainingIgnoreCase(String keyword);
}
