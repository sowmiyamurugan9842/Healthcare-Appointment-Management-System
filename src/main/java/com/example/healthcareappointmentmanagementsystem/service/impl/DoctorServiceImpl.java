package com.example.healthcareappointmentmanagementsystem.service.impl;

import com.example.healthcareappointmentmanagementsystem.dto.request.DoctorRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.DoctorResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Department;
import com.example.healthcareappointmentmanagementsystem.entity.Doctor;
import com.example.healthcareappointmentmanagementsystem.entity.User;
import com.example.healthcareappointmentmanagementsystem.exception.DuplicateResourceException;
import com.example.healthcareappointmentmanagementsystem.exception.ResourceNotFoundException;
import com.example.healthcareappointmentmanagementsystem.mapper.DoctorMapper;
import com.example.healthcareappointmentmanagementsystem.repository.DepartmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DoctorRepository;
import com.example.healthcareappointmentmanagementsystem.repository.UserRepository;
import com.example.healthcareappointmentmanagementsystem.service.DoctorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation handling Doctor profile management.
 */
@Service
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorMapper doctorMapper;

    /**
     * Constructor injection. Spring Boot injects the repositories and mapper.
     */
    public DoctorServiceImpl(DoctorRepository doctorRepository,
                             UserRepository userRepository,
                             DepartmentRepository departmentRepository,
                             DoctorMapper doctorMapper) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.doctorMapper = doctorMapper;
    }

    @Override
    public DoctorResponse createDoctor(DoctorRequest request) {
        // 1. Find User by ID
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));

        // 2. Find Department by ID
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        // 3. Check if this User is already assigned a Doctor profile
        if (doctorRepository.existsByUser(user)) {
            throw new DuplicateResourceException("A doctor profile already exists for user ID: " + request.getUserId());
        }

        // 4. Map DTO to Entity
        Doctor doctor = doctorMapper.toEntity(request, user, department);

        // 5. Save the doctor record
        Doctor savedDoctor = doctorRepository.save(doctor);

        // 6. Return response DTO
        return doctorMapper.toResponse(savedDoctor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> getAllDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        return doctors.stream()
                .map(doctorMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found with ID: " + id));
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> getDoctorsByDepartment(Long departmentId) {
        // 1. Verify that the department exists
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + departmentId));

        // 2. Fetch doctors by department
        List<Doctor> doctors = doctorRepository.findByDepartment(department);

        // 3. Return mapped DTOs
        return doctors.stream()
                .map(doctorMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> searchDoctorsBySpecialization(String specialization) {
        List<Doctor> doctors = doctorRepository.findBySpecializationContainingIgnoreCase(specialization);
        return doctors.stream()
                .map(doctorMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DoctorResponse updateDoctor(Long id, DoctorRequest request) {
        // 1. Find existing Doctor profile
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found with ID: " + id));

        // 2. Find User by ID
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));

        // 3. Find Department by ID
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        // 4. Validate duplicate doctor profile only if User is changed
        if (!doctor.getUser().getId().equals(request.getUserId())) {
            if (doctorRepository.existsByUser(user)) {
                throw new DuplicateResourceException("A doctor profile already exists for user ID: " + request.getUserId());
            }
        }

        // 5. Update fields on the existing managed entity
        doctor.setUser(user);
        doctor.setDepartment(department);
        doctor.setQualification(request.getQualification());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setExperienceYears(request.getExperienceYears());
        doctor.setConsultationFee(request.getConsultationFee());
        doctor.setAvailableFrom(request.getAvailableFrom());
        doctor.setAvailableTo(request.getAvailableTo());

        // 6. Save updates and return response DTO
        Doctor updatedDoctor = doctorRepository.save(doctor);
        return doctorMapper.toResponse(updatedDoctor);
    }

    @Override
    public void deleteDoctor(Long id) {
        // 1. Verify that the doctor exists
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found with ID: " + id));

        // 2. Delete Doctor (User record will be deleted automatically due to CascadeType.ALL)
        doctorRepository.delete(doctor);
    }
}
