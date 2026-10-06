package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.response.AppointmentResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.DoctorResponse;
import com.example.healthcareappointmentmanagementsystem.entity.*;
import com.example.healthcareappointmentmanagementsystem.exception.ResourceNotFoundException;
import com.example.healthcareappointmentmanagementsystem.exception.UnauthorizedException;
import com.example.healthcareappointmentmanagementsystem.mapper.AppointmentMapper;
import com.example.healthcareappointmentmanagementsystem.mapper.DoctorMapper;
import com.example.healthcareappointmentmanagementsystem.repository.*;
import com.example.healthcareappointmentmanagementsystem.service.impl.AppointmentServiceImpl;
import com.example.healthcareappointmentmanagementsystem.service.impl.DoctorServiceImpl;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorAppointmentResolutionTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorMapper doctorMapper;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private WaitlistService waitlistService;

    private DoctorServiceImpl doctorService;
    private AppointmentServiceImpl appointmentService;

    private Doctor doctorJenifer;
    private Doctor doctorHouse;
    private User userJenifer;
    private User userHouse;
    private User userAdmin;
    private Patient patientAlice;
    private Appointment apptJenifer;
    private Appointment apptHouse;

    @BeforeEach
    void setUp() {
        doctorService = new DoctorServiceImpl(
                doctorRepository,
                userRepository,
                departmentRepository,
                doctorMapper,
                appointmentRepository
        );

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

        userJenifer = User.builder()
                .id(2L)
                .email("jenifer@gmail.com")
                .firstName("Jeni")
                .lastName("fer")
                .role(Role.DOCTOR)
                .build();

        doctorJenifer = Doctor.builder()
                .id(1L)
                .user(userJenifer)
                .specialization("Cardiologist")
                .qualification("MD")
                .experienceYears(10)
                .consultationFee(150.0)
                .availableFrom(LocalTime.of(9, 0))
                .availableTo(LocalTime.of(17, 0))
                .build();

        userHouse = User.builder()
                .id(7L)
                .email("dr.house@careportal.com")
                .firstName("Gregory")
                .lastName("House")
                .role(Role.DOCTOR)
                .build();

        doctorHouse = Doctor.builder()
                .id(2L)
                .user(userHouse)
                .specialization("Neurologist")
                .qualification("MD")
                .experienceYears(20)
                .consultationFee(300.0)
                .availableFrom(LocalTime.of(10, 0))
                .availableTo(LocalTime.of(16, 0))
                .build();

        userAdmin = User.builder()
                .id(1L)
                .email("admin@hospital.com")
                .firstName("Admin")
                .lastName("User")
                .role(Role.ADMIN)
                .build();

        patientAlice = Patient.builder()
                .id(10L)
                .user(User.builder().id(20L).firstName("Alice").lastName("Smith").email("alice@patient.com").build())
                .build();

        apptJenifer = Appointment.builder()
                .id(101L)
                .doctor(doctorJenifer)
                .patient(patientAlice)
                .appointmentDate(LocalDate.now().plusDays(2))
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.PENDING)
                .reasonForVisit("Cardiology routine consultation")
                .build();

        apptHouse = Appointment.builder()
                .id(102L)
                .doctor(doctorHouse)
                .patient(patientAlice)
                .appointmentDate(LocalDate.now().plusDays(3))
                .appointmentTime(LocalTime.of(11, 0))
                .status(AppointmentStatus.PENDING)
                .reasonForVisit("Neurology complex diagnostic")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should resolve current doctor profile from JWT email")
    void testGetCurrentDoctorProfileSuccess() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "jenifer@gmail.com",
                        "password",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))
                )
        );

        DoctorResponse expectedResponse = DoctorResponse.builder()
                .id(1L)
                .fullName("Jeni fer")
                .email("jenifer@gmail.com")
                .specialization("Cardiologist")
                .build();

        when(doctorRepository.findByUser_Email("jenifer@gmail.com")).thenReturn(Optional.of(doctorJenifer));
        when(doctorMapper.toResponse(doctorJenifer)).thenReturn(expectedResponse);

        DoctorResponse actualResponse = doctorService.getCurrentDoctorProfile();

        assertNotNull(actualResponse);
        assertEquals(1L, actualResponse.getId());
        assertEquals("jenifer@gmail.com", actualResponse.getEmail());
        verify(doctorRepository, times(1)).findByUser_Email("jenifer@gmail.com");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException if unauthenticated when accessing current doctor profile")
    void testGetCurrentDoctorProfileUnauthenticated() {
        SecurityContextHolder.clearContext();

        assertThrows(UnauthorizedException.class, () -> doctorService.getCurrentDoctorProfile());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException if doctor record is not linked to authenticated user")
    void testGetCurrentDoctorProfileNotFound() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "unknown.doctor@gmail.com",
                        "password",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))
                )
        );

        when(doctorRepository.findByUser_Email("unknown.doctor@gmail.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> doctorService.getCurrentDoctorProfile());
    }

    @Test
    @DisplayName("Should automatically load doctor's appointments via getMyAppointmentsForDoctor")
    void testGetMyAppointmentsForDoctorSuccess() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "jenifer@gmail.com",
                        "password",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))
                )
        );

        when(doctorRepository.findByUser_Email("jenifer@gmail.com")).thenReturn(Optional.of(doctorJenifer));
        when(appointmentRepository.findByDoctor(doctorJenifer)).thenReturn(Collections.singletonList(apptJenifer));

        AppointmentResponse apptResp = AppointmentResponse.builder()
                .id(101L)
                .doctorName("Dr. Jeni fer")
                .patientName("Alice Smith")
                .status(AppointmentStatus.PENDING)
                .build();
        when(appointmentMapper.toResponse(apptJenifer)).thenReturn(apptResp);

        List<AppointmentResponse> results = appointmentService.getMyAppointmentsForDoctor();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(101L, results.get(0).getId());
        assertEquals("Dr. Jeni fer", results.get(0).getDoctorName());
    }

    @Test
    @DisplayName("Security: Doctor A cannot view Doctor B's appointments by changing doctorId")
    void testSecurityDoctorCannotViewOtherDoctorAppointments() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "jenifer@gmail.com",
                        "password",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))
                )
        );

        // Dr. Jenifer tries to pass doctorId = 2 (Dr. House)
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctorHouse));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {
            appointmentService.getAppointmentsByDoctor(2L);
        });

        assertTrue(ex.getMessage().contains("You are not authorized to view another doctor's appointments"));
        verify(appointmentRepository, never()).findByDoctor(doctorHouse);
    }

    @Test
    @DisplayName("Security: Doctor A can view their own appointments with explicit doctorId")
    void testDoctorCanViewOwnAppointmentsWithExplicitId() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "jenifer@gmail.com",
                        "password",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))
                )
        );

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctorJenifer));
        when(appointmentRepository.findByDoctor(doctorJenifer)).thenReturn(Collections.singletonList(apptJenifer));

        AppointmentResponse apptResp = AppointmentResponse.builder().id(101L).build();
        when(appointmentMapper.toResponse(apptJenifer)).thenReturn(apptResp);

        List<AppointmentResponse> results = appointmentService.getAppointmentsByDoctor(1L);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(101L, results.get(0).getId());
    }

    @Test
    @DisplayName("Security: Admin can view any doctor's appointments with doctorId")
    void testAdminCanViewAnyDoctorAppointments() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@hospital.com",
                        "password",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctorHouse));
        when(appointmentRepository.findByDoctor(doctorHouse)).thenReturn(Collections.singletonList(apptHouse));

        AppointmentResponse apptResp = AppointmentResponse.builder().id(102L).build();
        when(appointmentMapper.toResponse(apptHouse)).thenReturn(apptResp);

        List<AppointmentResponse> results = appointmentService.getAppointmentsByDoctor(2L);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(102L, results.get(0).getId());
    }

    @Test
    @DisplayName("Should confirm all pending appointments for authenticated doctor via confirmAllMyAppointments")
    void testConfirmAllMyAppointmentsSuccess() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "jenifer@gmail.com",
                        "password",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))
                )
        );

        when(doctorRepository.findByUser_Email("jenifer@gmail.com")).thenReturn(Optional.of(doctorJenifer));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctorJenifer));
        when(appointmentRepository.findByDoctorAndStatus(doctorJenifer, AppointmentStatus.PENDING))
                .thenReturn(Collections.singletonList(apptJenifer));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentResponse confirmedResp = AppointmentResponse.builder()
                .id(101L)
                .status(AppointmentStatus.CONFIRMED)
                .build();
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(confirmedResp);

        List<AppointmentResponse> result = appointmentService.confirmAllMyAppointments();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(AppointmentStatus.CONFIRMED, result.get(0).getStatus());
    }

    @Test
    @DisplayName("Security: Doctor A cannot confirm all appointments for Doctor B")
    void testSecurityDoctorCannotConfirmOtherDoctorAppointments() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "jenifer@gmail.com",
                        "password",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))
                )
        );

        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctorHouse));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {
            appointmentService.confirmAllAppointmentsByDoctor(2L);
        });

        assertTrue(ex.getMessage().contains("You are not authorized to confirm appointments for another doctor"));
    }
}
