package com.example.healthcareappointmentmanagementsystem.service.impl;

import com.example.healthcareappointmentmanagementsystem.dto.request.AppointmentRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.AppointmentResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Appointment;
import com.example.healthcareappointmentmanagementsystem.entity.AppointmentStatus;
import com.example.healthcareappointmentmanagementsystem.entity.Doctor;
import com.example.healthcareappointmentmanagementsystem.entity.Patient;
import com.example.healthcareappointmentmanagementsystem.exception.BadRequestException;
import com.example.healthcareappointmentmanagementsystem.exception.ResourceNotFoundException;
import com.example.healthcareappointmentmanagementsystem.mapper.AppointmentMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DoctorRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PatientRepository;
import com.example.healthcareappointmentmanagementsystem.service.AppointmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation handling medical Appointment scheduling and transitions.
 */
@Service
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentMapper appointmentMapper;

    /**
     * Constructor injection. Spring Boot injects the repositories and mapper.
     */
    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  DoctorRepository doctorRepository,
                                  PatientRepository patientRepository,
                                  AppointmentMapper appointmentMapper) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.appointmentMapper = appointmentMapper;
    }

    @Override
    public AppointmentResponse bookAppointment(AppointmentRequest request) {
        // Step 1: Find Doctor
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));

        // Step 2: Find Patient
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.getPatientId()));

        // Step 3: Validate appointment date is today or in the future
        if (request.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Appointment date must be today or in the future");
        }

        // Step 4: Validate appointment time is within doctor's available hours
        LocalTime time = request.getAppointmentTime();
        if (time.isBefore(doctor.getAvailableFrom()) || time.isAfter(doctor.getAvailableTo())) {
            throw new BadRequestException("Selected time " + time + " is outside the doctor's available hours: "
                    + doctor.getAvailableFrom() + " to " + doctor.getAvailableTo());
        }

        // Step 5: Check doctor is not already booked for the same date and time
        List<Appointment> doctorAppointments = appointmentRepository.findByDoctorAndAppointmentDate(doctor, request.getAppointmentDate());
        boolean hasDoctorClash = doctorAppointments.stream()
                .anyMatch(a -> a.getAppointmentTime().equals(time) && a.getStatus() != AppointmentStatus.CANCELLED);
        if (hasDoctorClash) {
            throw new BadRequestException("The doctor is already booked at " + time + " on " + request.getAppointmentDate());
        }

        // Step 6: Check patient does not already have another appointment on the same date
        List<Appointment> patientAppointments = appointmentRepository.findByPatientAndAppointmentDate(patient, request.getAppointmentDate());
        boolean hasPatientClash = patientAppointments.stream()
                .anyMatch(a -> a.getStatus() != AppointmentStatus.CANCELLED);
        if (hasPatientClash) {
            throw new BadRequestException("The patient already has an appointment booked on " + request.getAppointmentDate());
        }

        // Step 7: Create Appointment entity using Mapper
        Appointment appointment = appointmentMapper.toEntity(request, doctor, patient);

        // Step 8: Set status = PENDING
        appointment.setStatus(AppointmentStatus.PENDING);

        // Step 9: Save Appointment
        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Step 10: Return mapped response DTO
        return appointmentMapper.toResponse(savedAppointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAllAppointments() {
        List<Appointment> appointments = appointmentRepository.findAll();
        return appointments.stream()
                .map(appointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));
        return appointmentMapper.toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByPatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));

        List<Appointment> appointments = appointmentRepository.findByPatient(patient);
        return appointments.stream()
                .map(appointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        List<Appointment> appointments = appointmentRepository.findByDoctor(doctor);
        return appointments.stream()
                .map(appointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByStatus(AppointmentStatus status) {
        List<Appointment> appointments = appointmentRepository.findByStatus(status);
        return appointments.stream()
                .map(appointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AppointmentResponse confirmAppointment(Long appointmentId) {
        // Find appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // State Transition rule: Only PENDING -> CONFIRMED
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Only PENDING appointments can be confirmed. Current status: " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(savedAppointment);
    }

    @Override
    public AppointmentResponse cancelAppointment(Long appointmentId) {
        // Find appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // State Transition rule: Cannot cancel COMPLETED appointments
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel an appointment that is already completed.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(savedAppointment);
    }

    @Override
    public AppointmentResponse completeAppointment(Long appointmentId) {
        // Find appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // State Transition rule: Only CONFIRMED -> COMPLETED
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Only CONFIRMED appointments can be completed. Current status: " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(savedAppointment);
    }

    @Override
    public void deleteAppointment(Long id) {
        // Find appointment
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        // Delete appointment
        appointmentRepository.delete(appointment);
    }
}
