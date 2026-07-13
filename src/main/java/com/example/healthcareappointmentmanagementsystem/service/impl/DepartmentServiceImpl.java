package com.example.healthcareappointmentmanagementsystem.service.impl;

import com.example.healthcareappointmentmanagementsystem.dto.request.DepartmentRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.DepartmentResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Department;
import com.example.healthcareappointmentmanagementsystem.exception.DuplicateResourceException;
import com.example.healthcareappointmentmanagementsystem.exception.ResourceNotFoundException;
import com.example.healthcareappointmentmanagementsystem.mapper.DepartmentMapper;
import com.example.healthcareappointmentmanagementsystem.repository.DepartmentRepository;
import com.example.healthcareappointmentmanagementsystem.service.DepartmentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service implementation handling Hospital Department operations.
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    /**
     * Constructor injection. Spring Boot automatically injects repositories and mappers.
     */
    public DepartmentServiceImpl(DepartmentRepository departmentRepository, DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        // 1. Check if a department with this name already exists
        if (departmentRepository.existsByDepartmentName(request.getDepartmentName())) {
            throw new DuplicateResourceException("Department with this name already exists: " + request.getDepartmentName());
        }

        // 2. Map DTO to Entity
        Department department = departmentMapper.toEntity(request);

        // 3. Save the entity to the database
        Department savedDepartment = departmentRepository.save(department);

        // 4. Map the saved entity back to a response DTO
        return departmentMapper.toResponse(savedDepartment);
    }

    @Override
    public List<DepartmentResponse> getAllDepartments() {
        // 1. Fetch all departments from MySQL
        List<Department> departments = departmentRepository.findAll();

        // 2. Map the list of entities into a list of Response DTOs using Java Streams
        return departments.stream()
                .map(departmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        // 1. Lookup the department by ID or throw an exception if not found
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        // 2. Return mapped DTO response
        return departmentMapper.toResponse(department);
    }

    @Override
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        // 1. Find the target department
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        // 2. Check if another department already has the requested name
        Optional<Department> existingDepartment = departmentRepository.findByDepartmentName(request.getDepartmentName());
        if (existingDepartment.isPresent() && !existingDepartment.get().getId().equals(id)) {
            throw new DuplicateResourceException("Another department is already named: " + request.getDepartmentName());
        }

        // 3. Update the fields on the existing database entity
        department.setDepartmentName(request.getDepartmentName());
        department.setDescription(request.getDescription());

        // 4. Save updates and return the response DTO
        Department updatedDepartment = departmentRepository.save(department);
        return departmentMapper.toResponse(updatedDepartment);
    }

    @Override
    public void deleteDepartment(Long id) {
        // 1. Verify that the department exists
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        // 2. Delete the record from the database
        departmentRepository.delete(department);
    }
}
