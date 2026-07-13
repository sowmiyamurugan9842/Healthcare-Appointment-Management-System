package com.example.healthcareappointmentmanagementsystem.service.impl;

import com.example.healthcareappointmentmanagementsystem.dto.request.PatientRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.PatientResponse;
import com.example.healthcareappointmentmanagementsystem.entity.BloodGroup;
import com.example.healthcareappointmentmanagementsystem.entity.Gender;
import com.example.healthcareappointmentmanagementsystem.entity.Patient;
import com.example.healthcareappointmentmanagementsystem.entity.User;
import com.example.healthcareappointmentmanagementsystem.exception.DuplicateResourceException;
import com.example.healthcareappointmentmanagementsystem.exception.ResourceNotFoundException;
import com.example.healthcareappointmentmanagementsystem.mapper.PatientMapper;
import com.example.healthcareappointmentmanagementsystem.repository.PatientRepository;
import com.example.healthcareappointmentmanagementsystem.repository.UserRepository;
import com.example.healthcareappointmentmanagementsystem.service.PatientService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation handling Patient profile operations.
 */
@Service
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PatientMapper patientMapper;

    /**
     * Constructor injection. Spring Boot injects the repositories and mapper.
     */
    public PatientServiceImpl(PatientRepository patientRepository,
                              UserRepository userRepository,
                              PatientMapper patientMapper) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.patientMapper = patientMapper;
    }

    @Override
    public PatientResponse createPatient(PatientRequest request) {
        // 1. Find User by ID
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));

        // 2. Check if this User is already assigned a Patient profile
        if (patientRepository.existsByUser(user)) {
            throw new DuplicateResourceException("A patient profile already exists for user ID: " + request.getUserId());
        }

        // 3. Map DTO to Entity
        Patient patient = patientMapper.toEntity(request, user);

        // 4. Save the patient record
        Patient savedPatient = patientRepository.save(patient);

        // 5. Return response DTO
        return patientMapper.toResponse(savedPatient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getAllPatients() {
        List<Patient> patients = patientRepository.findAll();
        return patients.stream()
                .map(patientMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found with ID: " + id));
        return patientMapper.toResponse(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getPatientsByBloodGroup(BloodGroup bloodGroup) {
        List<Patient> patients = patientRepository.findByBloodGroup(bloodGroup);
        return patients.stream()
                .map(patientMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getPatientsByGender(Gender gender) {
        List<Patient> patients = patientRepository.findByGender(gender);
        return patients.stream()
                .map(patientMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PatientResponse updatePatient(Long id, PatientRequest request) {
        // 1. Find existing Patient profile
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found with ID: " + id));

        // 2. Find User by ID
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));

        // 3. Validate duplicate patient profile only if User is changed
        if (!patient.getUser().getId().equals(request.getUserId())) {
            if (patientRepository.existsByUser(user)) {
                throw new DuplicateResourceException("A patient profile already exists for user ID: " + request.getUserId());
            }
        }

        // 4. Update editable fields on the existing managed entity
        patient.setUser(user);
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setBloodGroup(request.getBloodGroup());
        patient.setAddress(request.getAddress());
        patient.setEmergencyContact(request.getEmergencyContact());
        patient.setAllergies(request.getAllergies());
        patient.setMedicalHistory(request.getMedicalHistory());

        // 5. Save updates and return response DTO
        Patient updatedPatient = patientRepository.save(patient);
        return patientMapper.toResponse(updatedPatient);
    }

    @Override
    public void deletePatient(Long id) {
        // 1. Verify that the patient exists
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found with ID: " + id));

        // 2. Delete Patient (User record will be deleted automatically due to CascadeType.ALL)
        patientRepository.delete(patient);
    }
}
