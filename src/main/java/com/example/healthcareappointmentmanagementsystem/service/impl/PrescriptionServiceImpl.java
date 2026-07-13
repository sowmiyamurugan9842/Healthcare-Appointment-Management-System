package com.example.healthcareappointmentmanagementsystem.service.impl;

import com.example.healthcareappointmentmanagementsystem.dto.request.PrescriptionRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.PrescriptionResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Appointment;
import com.example.healthcareappointmentmanagementsystem.entity.AppointmentStatus;
import com.example.healthcareappointmentmanagementsystem.entity.Prescription;
import com.example.healthcareappointmentmanagementsystem.exception.BadRequestException;
import com.example.healthcareappointmentmanagementsystem.exception.DuplicateResourceException;
import com.example.healthcareappointmentmanagementsystem.exception.ResourceNotFoundException;
import com.example.healthcareappointmentmanagementsystem.mapper.PrescriptionMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PrescriptionRepository;
import com.example.healthcareappointmentmanagementsystem.service.PrescriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation handling patient Prescription operations.
 */
@Service
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionMapper prescriptionMapper;

    /**
     * Constructor injection. Spring Boot injects dependencies.
     */
    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository,
                                   AppointmentRepository appointmentRepository,
                                   PrescriptionMapper prescriptionMapper) {
        this.prescriptionRepository = prescriptionRepository;
        this.appointmentRepository = appointmentRepository;
        this.prescriptionMapper = prescriptionMapper;
    }

    @Override
    public PrescriptionResponse createPrescription(PrescriptionRequest request) {
        // Step 1: Find Appointment
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + request.getAppointmentId()));

        // Step 2: Verify Appointment status == COMPLETED
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Cannot create a prescription for an uncompleted appointment. Current status: " + appointment.getStatus());
        }

        // Step 3: Check whether a Prescription already exists for this Appointment
        if (prescriptionRepository.findByAppointment(appointment).isPresent()) {
            throw new DuplicateResourceException("A prescription has already been issued for appointment ID: " + request.getAppointmentId());
        }

        // Step 4: Convert DTO to Entity using Mapper
        Prescription prescription = prescriptionMapper.toEntity(request, appointment);

        // Step 5: Save Prescription
        Prescription savedPrescription = prescriptionRepository.save(prescription);

        // Step 6: Return Response DTO
        return prescriptionMapper.toResponse(savedPrescription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getAllPrescriptions() {
        List<Prescription> prescriptions = prescriptionRepository.findAll();
        return prescriptions.stream()
                .map(prescriptionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionById(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + id));
        return prescriptionMapper.toResponse(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionByAppointment(Long appointmentId) {
        // Find Appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // Find Prescription by Appointment
        Prescription prescription = prescriptionRepository.findByAppointment(appointment)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found for appointment ID: " + appointmentId));

        return prescriptionMapper.toResponse(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPrescriptionsByNextVisitDate(LocalDate nextVisitDate) {
        List<Prescription> prescriptions = prescriptionRepository.findByNextVisitDate(nextVisitDate);
        return prescriptions.stream()
                .map(prescriptionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPrescriptionsBetweenDates(LocalDate startDate, LocalDate endDate) {
        List<Prescription> prescriptions = prescriptionRepository.findByNextVisitDateBetween(startDate, endDate);
        return prescriptions.stream()
                .map(prescriptionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PrescriptionResponse updatePrescription(Long id, PrescriptionRequest request) {
        // Find existing Prescription
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + id));

        // Update editable fields
        prescription.setDiagnosis(request.getDiagnosis());
        prescription.setMedications(request.getMedications());
        prescription.setDosageInstructions(request.getDosageInstructions());
        prescription.setAdditionalNotes(request.getAdditionalNotes());
        prescription.setNextVisitDate(request.getNextVisitDate());

        // Save updates
        Prescription updatedPrescription = prescriptionRepository.save(prescription);
        return prescriptionMapper.toResponse(updatedPrescription);
    }

    @Override
    public void deletePrescription(Long id) {
        // Find Prescription
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + id));

        // Delete Prescription
        prescriptionRepository.delete(prescription);
    }
}
