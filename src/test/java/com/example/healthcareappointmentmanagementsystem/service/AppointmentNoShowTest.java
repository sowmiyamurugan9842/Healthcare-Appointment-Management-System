package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.response.AppointmentResponse;
import com.example.healthcareappointmentmanagementsystem.entity.*;
import com.example.healthcareappointmentmanagementsystem.exception.BadRequestException;
import com.example.healthcareappointmentmanagementsystem.exception.UnauthorizedException;
import com.example.healthcareappointmentmanagementsystem.mapper.AppointmentMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DoctorRepository;
import com.example.healthcareappointmentmanagementsystem.repository.NotificationRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PatientRepository;
import com.example.healthcareappointmentmanagementsystem.service.impl.AppointmentServiceImpl;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentNoShowTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private WaitlistService waitlistService;

    private AppointmentServiceImpl appointmentService;

    private Doctor testDoctor;
    private Patient testPatient;
    private User doctorUser;
    private User patientUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentServiceImpl(
                appointmentRepository,
                doctorRepository,
                patientRepository,
                appointmentMapper,
                notificationService,
                notificationRepository,
                waitlistService
        );

        doctorUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Smith")
                .email("doctor@careportal.com")
                .role(Role.DOCTOR)
                .build();

        testDoctor = Doctor.builder()
                .id(10L)
                .user(doctorUser)
                .qualification("MBBS, MD")
                .specialization("Cardiology")
                .experienceYears(10)
                .consultationFee(100.0)
                .availableFrom(LocalTime.of(9, 0))
                .availableTo(LocalTime.of(17, 0))
                .build();

        patientUser = User.builder()
                .id(2L)
                .firstName("Alice")
                .lastName("Walker")
                .email("patient@careportal.com")
                .role(Role.PATIENT)
                .build();

        testPatient = Patient.builder()
                .id(20L)
                .user(patientUser)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.FEMALE)
                .bloodGroup(BloodGroup.O_POSITIVE)
                .address("123 Health Ave")
                .emergencyContact("1234567890")
                .build();

        adminUser = User.builder()
                .id(3L)
                .firstName("Super")
                .lastName("Admin")
                .email("admin@careportal.com")
                .role(Role.ADMIN)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(String email, Role role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("1. Confirmed appointment can become NO_SHOW when marked by doctor")
    void testConfirmedAppointment_CanBecomeNoShow_ByDoctor() {
        authenticateUser("doctor@careportal.com", Role.DOCTOR);

        Appointment appt = Appointment.builder()
                .id(100L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(LocalDate.now())
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.CONFIRMED)
                .build();

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse expectedResponse = AppointmentResponse.builder()
                .id(100L)
                .status(AppointmentStatus.NO_SHOW)
                .markedNoShowBy("doctor@careportal.com")
                .noShowReason("Patient did not arrive")
                .build();
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(expectedResponse);

        AppointmentResponse result = appointmentService.markAppointmentAsNoShow(100L, "Patient did not arrive");

        assertNotNull(result);
        assertEquals(AppointmentStatus.NO_SHOW, appt.getStatus());
        assertEquals("doctor@careportal.com", appt.getMarkedNoShowBy());
        assertEquals("Patient did not arrive", appt.getNoShowReason());
        assertNotNull(appt.getNoShowAt());

        verify(notificationService, times(1)).createNoShowNotification(appt);
        verify(waitlistService, times(1)).processWaitlistForSlot(testDoctor, LocalDate.now(), LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("2. Completed appointment cannot become NO_SHOW")
    void testCompletedAppointment_CannotBecomeNoShow() {
        authenticateUser("doctor@careportal.com", Role.DOCTOR);

        Appointment appt = Appointment.builder()
                .id(101L)
                .doctor(testDoctor)
                .patient(testPatient)
                .status(AppointmentStatus.COMPLETED)
                .build();

        when(appointmentRepository.findById(101L)).thenReturn(Optional.of(appt));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                appointmentService.markAppointmentAsNoShow(101L, "Did not attend")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("cannot mark a completed appointment as no-show"));
        verify(appointmentRepository, never()).save(any());
        verify(notificationService, never()).createNoShowNotification(any());
    }

    @Test
    @DisplayName("3. Cancelled appointment cannot become NO_SHOW")
    void testCancelledAppointment_CannotBecomeNoShow() {
        authenticateUser("doctor@careportal.com", Role.DOCTOR);

        Appointment appt = Appointment.builder()
                .id(102L)
                .doctor(testDoctor)
                .patient(testPatient)
                .status(AppointmentStatus.CANCELLED)
                .build();

        when(appointmentRepository.findById(102L)).thenReturn(Optional.of(appt));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                appointmentService.markAppointmentAsNoShow(102L, "Did not attend")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("cannot mark a cancelled appointment as no-show"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("4. Already NO_SHOW appointment is not processed again")
    void testAlreadyNoShow_CannotBeMarkedAgain() {
        authenticateUser("doctor@careportal.com", Role.DOCTOR);

        Appointment appt = Appointment.builder()
                .id(103L)
                .doctor(testDoctor)
                .patient(testPatient)
                .status(AppointmentStatus.NO_SHOW)
                .build();

        when(appointmentRepository.findById(103L)).thenReturn(Optional.of(appt));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                appointmentService.markAppointmentAsNoShow(103L, "Did not attend")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("already marked as no-show"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. Unauthorized patient cannot mark NO_SHOW")
    void testUnauthorizedPatient_CannotMarkNoShow() {
        authenticateUser("patient@careportal.com", Role.PATIENT);

        Appointment appt = Appointment.builder()
                .id(104L)
                .doctor(testDoctor)
                .patient(testPatient)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        when(appointmentRepository.findById(104L)).thenReturn(Optional.of(appt));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                appointmentService.markAppointmentAsNoShow(104L, "Mark self as no-show")
        );

        assertTrue(ex.getMessage().contains("Patients are not authorized"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. Doctor cannot mark another doctor's appointment as NO_SHOW")
    void testDoctor_CannotMarkOtherDoctorsAppointment() {
        authenticateUser("otherdoctor@careportal.com", Role.DOCTOR);

        Appointment appt = Appointment.builder()
                .id(105L)
                .doctor(testDoctor) // owned by doctor@careportal.com
                .patient(testPatient)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        when(appointmentRepository.findById(105L)).thenReturn(Optional.of(appt));

        assertThrows(UnauthorizedException.class, () ->
                appointmentService.markAppointmentAsNoShow(105L, "Did not attend")
        );

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Admin can manage NO_SHOW according to existing permissions")
    void testAdmin_CanMarkAnyAppointmentNoShow() {
        authenticateUser("admin@careportal.com", Role.ADMIN);

        Appointment appt = Appointment.builder()
                .id(106L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(LocalDate.now())
                .appointmentTime(LocalTime.of(14, 0))
                .status(AppointmentStatus.CONFIRMED)
                .build();

        when(appointmentRepository.findById(106L)).thenReturn(Optional.of(appt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentResponse expectedResponse = AppointmentResponse.builder()
                .id(106L)
                .status(AppointmentStatus.NO_SHOW)
                .markedNoShowBy("admin@careportal.com")
                .build();
        when(appointmentMapper.toResponse(any())).thenReturn(expectedResponse);

        AppointmentResponse result = appointmentService.markAppointmentAsNoShow(106L, "Admin confirmed non-attendance");

        assertNotNull(result);
        assertEquals(AppointmentStatus.NO_SHOW, appt.getStatus());
        assertEquals("admin@careportal.com", appt.getMarkedNoShowBy());
        verify(notificationService, times(1)).createNoShowNotification(appt);
    }

    @Test
    @DisplayName("8. NO_SHOW triggers existing waitlist automation when appropriate")
    void testNoShow_TriggersWaitlistAutomation() {
        authenticateUser("doctor@careportal.com", Role.DOCTOR);

        LocalDate futureDate = LocalDate.now().plusDays(2);
        LocalTime apptTime = LocalTime.of(11, 30);

        Appointment appt = Appointment.builder()
                .id(107L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(futureDate)
                .appointmentTime(apptTime)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        when(appointmentRepository.findById(107L)).thenReturn(Optional.of(appt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));
        when(appointmentMapper.toResponse(any())).thenReturn(AppointmentResponse.builder().id(107L).status(AppointmentStatus.NO_SHOW).build());

        appointmentService.markAppointmentAsNoShow(107L, "Patient called to state will not attend");

        verify(waitlistService, times(1)).processWaitlistForSlot(testDoctor, futureDate, apptTime);
    }

    @Test
    @DisplayName("9. No-show count calculation supports filtering by doctor, patient, date range")
    void testNoShowCount_Calculation() {
        LocalDate today = LocalDate.now();
        Appointment a1 = Appointment.builder()
                .id(1L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(today)
                .status(AppointmentStatus.NO_SHOW)
                .build();

        Appointment a2 = Appointment.builder()
                .id(2L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(today.minusDays(5))
                .status(AppointmentStatus.NO_SHOW)
                .build();

        when(appointmentRepository.findByStatus(AppointmentStatus.NO_SHOW))
                .thenReturn(Arrays.asList(a1, a2));

        long count = appointmentService.getNoShowCount(10L, 20L, today.minusDays(10), today);

        assertEquals(2L, count);
    }

    @Test
    @DisplayName("10. Generic updateAppointmentStatus with NO_SHOW executes no-show workflow")
    void testUpdateAppointmentStatus_TransitionToNoShow() {
        authenticateUser("doctor@careportal.com", Role.DOCTOR);

        Appointment appt = Appointment.builder()
                .id(108L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(LocalDate.now())
                .appointmentTime(LocalTime.of(9, 30))
                .status(AppointmentStatus.CONFIRMED)
                .build();

        when(appointmentRepository.findById(108L)).thenReturn(Optional.of(appt));
        when(appointmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(appointmentMapper.toResponse(any())).thenReturn(AppointmentResponse.builder().id(108L).status(AppointmentStatus.NO_SHOW).build());

        AppointmentResponse res = appointmentService.updateAppointmentStatus(108L, "NO_SHOW");

        assertNotNull(res);
        assertEquals(AppointmentStatus.NO_SHOW, appt.getStatus());
        verify(notificationService, times(1)).createNoShowNotification(appt);
    }
}
