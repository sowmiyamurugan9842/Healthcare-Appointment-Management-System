package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.request.AppointmentRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.AppointmentResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.PatientResponse;
import com.example.healthcareappointmentmanagementsystem.entity.*;
import com.example.healthcareappointmentmanagementsystem.exception.BadRequestException;
import com.example.healthcareappointmentmanagementsystem.exception.ResourceNotFoundException;
import com.example.healthcareappointmentmanagementsystem.exception.UnauthorizedException;
import com.example.healthcareappointmentmanagementsystem.mapper.AppointmentMapper;
import com.example.healthcareappointmentmanagementsystem.mapper.PatientMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DoctorRepository;
import com.example.healthcareappointmentmanagementsystem.repository.NotificationRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PatientRepository;
import com.example.healthcareappointmentmanagementsystem.repository.UserRepository;
import com.example.healthcareappointmentmanagementsystem.service.impl.AppointmentServiceImpl;
import com.example.healthcareappointmentmanagementsystem.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientAppointmentBookingTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private PatientMapper patientMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private WaitlistService waitlistService;

    private AppointmentServiceImpl appointmentService;
    private PatientServiceImpl patientService;

    private Doctor testDoctor;
    private Patient testPatient;
    private User patientUser;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentServiceImpl(
                appointmentRepository,
                doctorRepository,
                patientRepository,
                appointmentMapper,
                notificationService,
                notificationRepository,
                waitlistService,
                null
        );

        patientService = new PatientServiceImpl(
                patientRepository,
                userRepository,
                patientMapper
        );

        // Setup Test Doctor
        User doctorUser = User.builder()
                .id(10L)
                .email("dr.smith@careportal.com")
                .firstName("John")
                .lastName("Smith")
                .role(Role.DOCTOR)
                .build();

        testDoctor = Doctor.builder()
                .id(100L)
                .user(doctorUser)
                .specialization("Cardiology")
                .qualification("MD, FACC")
                .availableFrom(LocalTime.of(9, 0))
                .availableTo(LocalTime.of(17, 0))
                .consultationFee(150.0)
                .build();

        // Setup Test Patient User and Patient
        patientUser = User.builder()
                .id(20L)
                .email("patient.sarah@careportal.com")
                .firstName("Sarah")
                .lastName("Jenkins")
                .role(Role.PATIENT)
                .build();

        testPatient = Patient.builder()
                .id(200L)
                .user(patientUser)
                .gender(Gender.FEMALE)
                .bloodGroup(BloodGroup.O_POSITIVE)
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .address("123 Health Ave")
                .emergencyContact("9876543210")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsPatient(String email) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authenticateAsAdmin(String email) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("PATIENT booking: Automatically resolves patient from JWT authentication without manual patientId")
    void bookAppointment_asPatient_determinesPatientAutomaticallyFromJwt() {
        authenticateAsPatient("patient.sarah@careportal.com");

        AppointmentRequest request = AppointmentRequest.builder()
                .doctorId(100L)
                .patientId(null) // Patient does not provide patientId
                .appointmentDate(LocalDate.now().plusDays(2))
                .appointmentTime(LocalTime.of(10, 0))
                .reasonForVisit("Routine heart health checkup")
                .build();

        when(doctorRepository.findById(100L)).thenReturn(Optional.of(testDoctor));
        when(patientRepository.findByUser_Email("patient.sarah@careportal.com")).thenReturn(Optional.of(testPatient));
        when(appointmentRepository.findByDoctorAndAppointmentDate(testDoctor, request.getAppointmentDate())).thenReturn(Collections.emptyList());
        when(appointmentRepository.findByPatientAndAppointmentDate(testPatient, request.getAppointmentDate())).thenReturn(Collections.emptyList());

        Appointment createdEntity = Appointment.builder()
                .id(501L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .reasonForVisit(request.getReasonForVisit())
                .status(AppointmentStatus.PENDING)
                .build();

        AppointmentResponse expectedResponse = AppointmentResponse.builder()
                .id(501L)
                .doctorName("Dr. John Smith")
                .patientName("Sarah Jenkins")
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .status(AppointmentStatus.PENDING)
                .build();

        when(appointmentMapper.toEntity(any(AppointmentRequest.class), eq(testDoctor), eq(testPatient))).thenReturn(createdEntity);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(createdEntity);
        when(appointmentMapper.toResponse(createdEntity)).thenReturn(expectedResponse);

        AppointmentResponse actual = appointmentService.bookAppointment(request);

        assertNotNull(actual);
        assertEquals(501L, actual.getId());
        assertEquals("Sarah Jenkins", actual.getPatientName());
        assertEquals(200L, request.getPatientId()); // Verifies patientId was populated

        verify(patientRepository).findByUser_Email("patient.sarah@careportal.com");
        verify(appointmentRepository).save(createdEntity);
    }

    @Test
    @DisplayName("PATIENT booking: Overrides and ignores any foreign patientId sent from client")
    void bookAppointment_asPatient_overridesSuppliedDifferentPatientId() {
        authenticateAsPatient("patient.sarah@careportal.com");

        // Malicious or mismatched patientId 999L sent in request
        AppointmentRequest request = AppointmentRequest.builder()
                .doctorId(100L)
                .patientId(999L)
                .appointmentDate(LocalDate.now().plusDays(3))
                .appointmentTime(LocalTime.of(11, 0))
                .reasonForVisit("Follow up consultation")
                .build();

        when(doctorRepository.findById(100L)).thenReturn(Optional.of(testDoctor));
        when(patientRepository.findByUser_Email("patient.sarah@careportal.com")).thenReturn(Optional.of(testPatient));
        when(appointmentRepository.findByDoctorAndAppointmentDate(testDoctor, request.getAppointmentDate())).thenReturn(Collections.emptyList());
        when(appointmentRepository.findByPatientAndAppointmentDate(testPatient, request.getAppointmentDate())).thenReturn(Collections.emptyList());

        Appointment createdEntity = Appointment.builder()
                .id(502L)
                .doctor(testDoctor)
                .patient(testPatient)
                .status(AppointmentStatus.PENDING)
                .build();

        when(appointmentMapper.toEntity(any(AppointmentRequest.class), eq(testDoctor), eq(testPatient))).thenReturn(createdEntity);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(createdEntity);
        when(appointmentMapper.toResponse(any())).thenReturn(AppointmentResponse.builder().id(502L).patientName("Sarah Jenkins").build());

        AppointmentResponse response = appointmentService.bookAppointment(request);

        assertNotNull(response);
        assertEquals(200L, request.getPatientId()); // Overridden to authenticated patient's ID
        verify(appointmentMapper).toEntity(any(AppointmentRequest.class), eq(testDoctor), eq(testPatient));
    }

    @Test
    @DisplayName("PATIENT booking: Throws ResourceNotFoundException when user account has no Patient profile")
    void bookAppointment_asPatient_withoutPatientProfile_throwsResourceNotFoundException() {
        authenticateAsPatient("new.user@careportal.com");

        AppointmentRequest request = AppointmentRequest.builder()
                .doctorId(100L)
                .appointmentDate(LocalDate.now().plusDays(1))
                .appointmentTime(LocalTime.of(14, 0))
                .reasonForVisit("Initial visit")
                .build();

        when(doctorRepository.findById(100L)).thenReturn(Optional.of(testDoctor));
        when(patientRepository.findByUser_Email("new.user@careportal.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> appointmentService.bookAppointment(request));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("ADMIN booking: Allows booking for an explicitly specified patientId")
    void bookAppointment_asAdmin_withValidPatientId_succeeds() {
        authenticateAsAdmin("admin@careportal.com");

        AppointmentRequest request = AppointmentRequest.builder()
                .doctorId(100L)
                .patientId(200L)
                .appointmentDate(LocalDate.now().plusDays(2))
                .appointmentTime(LocalTime.of(15, 0))
                .reasonForVisit("Admin scheduled checkup")
                .build();

        when(doctorRepository.findById(100L)).thenReturn(Optional.of(testDoctor));
        when(patientRepository.findById(200L)).thenReturn(Optional.of(testPatient));
        when(appointmentRepository.findByDoctorAndAppointmentDate(testDoctor, request.getAppointmentDate())).thenReturn(Collections.emptyList());
        when(appointmentRepository.findByPatientAndAppointmentDate(testPatient, request.getAppointmentDate())).thenReturn(Collections.emptyList());

        Appointment createdEntity = Appointment.builder().id(503L).doctor(testDoctor).patient(testPatient).status(AppointmentStatus.PENDING).build();
        when(appointmentMapper.toEntity(any(AppointmentRequest.class), eq(testDoctor), eq(testPatient))).thenReturn(createdEntity);
        when(appointmentRepository.save(any())).thenReturn(createdEntity);
        when(appointmentMapper.toResponse(createdEntity)).thenReturn(AppointmentResponse.builder().id(503L).patientName("Sarah Jenkins").build());

        AppointmentResponse response = appointmentService.bookAppointment(request);

        assertNotNull(response);
        assertEquals(503L, response.getId());
        verify(patientRepository).findById(200L);
    }

    @Test
    @DisplayName("ADMIN booking: Throws BadRequestException when patientId is missing")
    void bookAppointment_asAdmin_withoutPatientId_throwsBadRequestException() {
        authenticateAsAdmin("admin@careportal.com");

        AppointmentRequest request = AppointmentRequest.builder()
                .doctorId(100L)
                .patientId(null)
                .appointmentDate(LocalDate.now().plusDays(2))
                .appointmentTime(LocalTime.of(15, 0))
                .reasonForVisit("Admin scheduled checkup")
                .build();

        when(doctorRepository.findById(100L)).thenReturn(Optional.of(testDoctor));

        assertThrows(BadRequestException.class, () -> appointmentService.bookAppointment(request));
    }

    @Test
    @DisplayName("Patient service: getCurrentPatientProfile returns authenticated patient's profile")
    void patientService_getCurrentPatientProfile_succeeds() {
        authenticateAsPatient("patient.sarah@careportal.com");

        when(patientRepository.findByUser_Email("patient.sarah@careportal.com")).thenReturn(Optional.of(testPatient));
        PatientResponse expected = PatientResponse.builder()
                .id(200L)
                .fullName("Sarah Jenkins")
                .email("patient.sarah@careportal.com")
                .build();
        when(patientMapper.toResponse(testPatient)).thenReturn(expected);

        PatientResponse actual = patientService.getCurrentPatientProfile();

        assertNotNull(actual);
        assertEquals(200L, actual.getId());
        assertEquals("Sarah Jenkins", actual.getFullName());
    }

    @Test
    @DisplayName("Patient service: getCurrentPatientProfile throws UnauthorizedException when unauthenticated")
    void patientService_getCurrentPatientProfile_unauthenticated_throwsUnauthorizedException() {
        SecurityContextHolder.clearContext();

        assertThrows(UnauthorizedException.class, () -> patientService.getCurrentPatientProfile());
    }

    @Test
    @DisplayName("Patient My Appointments: getMyAppointmentsForPatient resolves profile and returns appointments")
    void getMyAppointmentsForPatient_authenticated_succeeds() {
        authenticateAsPatient("patient.sarah@careportal.com");

        when(patientRepository.findByUser_Email("patient.sarah@careportal.com")).thenReturn(Optional.of(testPatient));
        Appointment appt = Appointment.builder().id(999L).doctor(testDoctor).patient(testPatient).status(AppointmentStatus.CONFIRMED).build();
        when(appointmentRepository.findByPatient(testPatient)).thenReturn(List.of(appt));
        AppointmentResponse res = AppointmentResponse.builder().id(999L).patientName("Sarah Jenkins").build();
        when(appointmentMapper.toResponse(appt)).thenReturn(res);

        List<AppointmentResponse> result = appointmentService.getMyAppointmentsForPatient();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(999L, result.get(0).getId());
    }

    @Test
    @DisplayName("Patient Security: getAppointmentsByPatient throws UnauthorizedException when Patient A queries Patient B")
    void getAppointmentsByPatient_patientQueryingOtherPatient_throwsUnauthorized() {
        authenticateAsPatient("patient.sarah@careportal.com");

        User otherUser = User.builder().id(999L).email("other.patient@careportal.com").build();
        Patient otherPatient = Patient.builder().id(999L).user(otherUser).build();
        when(patientRepository.findById(999L)).thenReturn(Optional.of(otherPatient));

        assertThrows(UnauthorizedException.class, () -> appointmentService.getAppointmentsByPatient(999L));
    }

    @Test
    @DisplayName("Patient Security: getAppointmentsByPatient succeeds when Admin queries any patient")
    void getAppointmentsByPatient_adminQueryingAnyPatient_succeeds() {
        authenticateAsAdmin("admin@careportal.com");

        when(patientRepository.findById(200L)).thenReturn(Optional.of(testPatient));
        Appointment appt = Appointment.builder().id(999L).doctor(testDoctor).patient(testPatient).status(AppointmentStatus.CONFIRMED).build();
        when(appointmentRepository.findByPatient(testPatient)).thenReturn(List.of(appt));
        AppointmentResponse res = AppointmentResponse.builder().id(999L).patientName("Sarah Jenkins").build();
        when(appointmentMapper.toResponse(appt)).thenReturn(res);

        List<AppointmentResponse> result = appointmentService.getAppointmentsByPatient(200L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(999L, result.get(0).getId());
    }
}
