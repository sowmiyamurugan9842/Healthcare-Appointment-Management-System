package com.example.healthcareappointmentmanagementsystem.service.impl;

import com.example.healthcareappointmentmanagementsystem.dto.request.DoctorRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.DoctorAvailableSlotsResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.DoctorResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Appointment;
import com.example.healthcareappointmentmanagementsystem.entity.AppointmentStatus;
import com.example.healthcareappointmentmanagementsystem.entity.Department;
import com.example.healthcareappointmentmanagementsystem.entity.Doctor;
import com.example.healthcareappointmentmanagementsystem.entity.User;
import com.example.healthcareappointmentmanagementsystem.exception.BadRequestException;
import com.example.healthcareappointmentmanagementsystem.exception.DuplicateResourceException;
import com.example.healthcareappointmentmanagementsystem.exception.ResourceNotFoundException;
import com.example.healthcareappointmentmanagementsystem.exception.UnauthorizedException;
import com.example.healthcareappointmentmanagementsystem.mapper.DoctorMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DepartmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DoctorRepository;
import com.example.healthcareappointmentmanagementsystem.repository.UserRepository;
import com.example.healthcareappointmentmanagementsystem.service.DoctorService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    private final AppointmentRepository appointmentRepository;

    /**
     * Constructor injection. Spring Boot injects the repositories and mapper.
     */
    public DoctorServiceImpl(DoctorRepository doctorRepository,
                             UserRepository userRepository,
                             DepartmentRepository departmentRepository,
                             DoctorMapper doctorMapper,
                             AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.doctorMapper = doctorMapper;
        this.appointmentRepository = appointmentRepository;
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

    @Override
    @Transactional(readOnly = true)
    public DoctorAvailableSlotsResponse getAvailableSlots(Long doctorId, LocalDate date) {
        // 1. Validate date input
        if (date == null) {
            throw new BadRequestException("Appointment date is required");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Appointment date must be today or in the future");
        }

        // 2. Fetch Doctor or throw ResourceNotFoundException
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found with ID: " + doctorId));

        List<String> availableSlots = new ArrayList<>();
        LocalTime from = doctor.getAvailableFrom();
        LocalTime to = doctor.getAvailableTo();

        // 3. Generate slots if working hours are defined and valid
        if (from != null && to != null && from.isBefore(to)) {
            // Find existing appointments for this doctor on the requested date
            List<Appointment> existingAppointments = appointmentRepository.findByDoctorAndAppointmentDate(doctor, date);
            
            // Active appointments (PENDING, CONFIRMED, COMPLETED) block slots; CANCELLED, EXPIRED, and NO_SHOW do not
            Set<String> bookedSlots = existingAppointments.stream()
                    .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.EXPIRED && a.getStatus() != AppointmentStatus.NO_SHOW)
                    .map(a -> a.getAppointmentTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                    .collect(Collectors.toSet());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime currentSlot = from;
            while (!currentSlot.plusMinutes(30).isAfter(to)) {
                String slotStr = currentSlot.format(formatter);
                if (!bookedSlots.contains(slotStr)) {
                    availableSlots.add(slotStr);
                }
                currentSlot = currentSlot.plusMinutes(30);
            }
        }

        return DoctorAvailableSlotsResponse.builder()
                .doctorId(doctorId)
                .date(date)
                .availableSlots(availableSlots)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getCurrentDoctorProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new UnauthorizedException("User is not authenticated. Please log in.");
        }

        String email = auth.getName();
        Doctor doctor = doctorRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for user: " + email));

        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorByUserId(Long userId) {
        Doctor doctor = doctorRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for user ID: " + userId));
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorByUserEmail(String email) {
        Doctor doctor = doctorRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for email: " + email));
        return doctorMapper.toResponse(doctor);
    }
}

