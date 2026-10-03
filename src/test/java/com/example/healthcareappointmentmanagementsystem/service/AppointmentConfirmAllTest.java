package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.response.AppointmentResponse;
import com.example.healthcareappointmentmanagementsystem.entity.*;
import com.example.healthcareappointmentmanagementsystem.exception.BadRequestException;
import com.example.healthcareappointmentmanagementsystem.exception.ResourceNotFoundException;
import com.example.healthcareappointmentmanagementsystem.mapper.AppointmentMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DoctorRepository;
import com.example.healthcareappointmentmanagementsystem.repository.NotificationRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PatientRepository;
import com.example.healthcareappointmentmanagementsystem.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppointmentConfirmAllTest {

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

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private Doctor doctor;
    private Patient patient;

    @BeforeEach
    void setUp() {
        doctor = Doctor.builder()
                .id(4L)
                .qualification("MD")
                .specialization("Cardiologist")
                .availableFrom(LocalTime.of(9, 0))
                .availableTo(LocalTime.of(17, 0))
                .build();

        patient = Patient.builder()
                .id(10L)
                .build();
    }

    @Test
    @DisplayName("TEST 1: Multiple PENDING appointments for doctor -> All become CONFIRMED")
    void testConfirmAll_MultiplePendingAppointments_AllConfirmed() {
        Appointment appt1 = Appointment.builder()
                .id(101L)
                .doctor(doctor)
                .patient(patient)
                .appointmentDate(LocalDate.now().plusDays(1))
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment appt2 = Appointment.builder()
                .id(102L)
                .doctor(doctor)
                .patient(patient)
                .appointmentDate(LocalDate.now().plusDays(1))
                .appointmentTime(LocalTime.of(11, 0))
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment appt3 = Appointment.builder()
                .id(103L)
                .doctor(doctor)
                .patient(patient)
                .appointmentDate(LocalDate.now().plusDays(2))
                .appointmentTime(LocalTime.of(14, 0))
                .status(AppointmentStatus.PENDING)
                .build();

        when(doctorRepository.findById(4L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByDoctorAndStatus(doctor, AppointmentStatus.PENDING))
                .thenReturn(Arrays.asList(appt1, appt2, appt3));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));
        when(appointmentMapper.toResponse(any(Appointment.class))).thenAnswer(i -> {
            Appointment a = i.getArgument(0);
            return AppointmentResponse.builder()
                    .id(a.getId())
                    .status(a.getStatus())
                    .build();
        });

        List<AppointmentResponse> confirmedList = appointmentService.confirmAllAppointmentsByDoctor(4L);

        assertEquals(3, confirmedList.size());
        assertEquals(AppointmentStatus.CONFIRMED, appt1.getStatus());
        assertEquals(AppointmentStatus.CONFIRMED, appt2.getStatus());
        assertEquals(AppointmentStatus.CONFIRMED, appt3.getStatus());
        verify(appointmentRepository, times(3)).save(any(Appointment.class));
    }

    @Test
    @DisplayName("TEST 2: Mixed status appointments -> Only PENDING becomes CONFIRMED, others unchanged")
    void testConfirmAll_MixedStatusAppointments_OnlyPendingConfirmed() {
        Appointment pendingAppt = Appointment.builder()
                .id(201L)
                .doctor(doctor)
                .patient(patient)
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment confirmedAppt = Appointment.builder()
                .id(202L)
                .doctor(doctor)
                .patient(patient)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        Appointment completedAppt = Appointment.builder()
                .id(203L)
                .doctor(doctor)
                .patient(patient)
                .status(AppointmentStatus.COMPLETED)
                .build();

        Appointment cancelledAppt = Appointment.builder()
                .id(204L)
                .doctor(doctor)
                .patient(patient)
                .status(AppointmentStatus.CANCELLED)
                .build();

        Appointment expiredAppt = Appointment.builder()
                .id(205L)
                .doctor(doctor)
                .patient(patient)
                .status(AppointmentStatus.EXPIRED)
                .build();

        when(doctorRepository.findById(4L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByDoctorAndStatus(doctor, AppointmentStatus.PENDING))
                .thenReturn(Collections.singletonList(pendingAppt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));
        when(appointmentMapper.toResponse(any(Appointment.class))).thenAnswer(i -> {
            Appointment a = i.getArgument(0);
            return AppointmentResponse.builder()
                    .id(a.getId())
                    .status(a.getStatus())
                    .build();
        });

        List<AppointmentResponse> result = appointmentService.confirmAllAppointmentsByDoctor(4L);

        assertEquals(1, result.size());
        assertEquals(AppointmentStatus.CONFIRMED, pendingAppt.getStatus());

        // Verify other appointments stayed unchanged
        assertEquals(AppointmentStatus.CONFIRMED, confirmedAppt.getStatus());
        assertEquals(AppointmentStatus.COMPLETED, completedAppt.getStatus());
        assertEquals(AppointmentStatus.CANCELLED, cancelledAppt.getStatus());
        assertEquals(AppointmentStatus.EXPIRED, expiredAppt.getStatus());

        verify(appointmentRepository, times(1)).save(pendingAppt);
    }

    @Test
    @DisplayName("TEST 3: No PENDING appointments -> Returns empty list, no errors")
    void testConfirmAll_NoPendingAppointments_ReturnsEmptyList() {
        when(doctorRepository.findById(4L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByDoctorAndStatus(doctor, AppointmentStatus.PENDING))
                .thenReturn(Collections.emptyList());

        List<AppointmentResponse> result = appointmentService.confirmAllAppointmentsByDoctor(4L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("TEST 4: Non-existent doctor ID -> Throws ResourceNotFoundException")
    void testConfirmAll_DoctorNotFound_ThrowsResourceNotFoundException() {
        when(doctorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            appointmentService.confirmAllAppointmentsByDoctor(999L);
        });

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("TEST 5: Individual confirmAppointment still works as expected")
    void testIndividualConfirmAppointment_StillWorks() {
        Appointment pendingAppt = Appointment.builder()
                .id(301L)
                .doctor(doctor)
                .patient(patient)
                .status(AppointmentStatus.PENDING)
                .build();

        when(appointmentRepository.findById(301L)).thenReturn(Optional.of(pendingAppt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(
                AppointmentResponse.builder().id(301L).status(AppointmentStatus.CONFIRMED).build()
        );

        AppointmentResponse response = appointmentService.confirmAppointment(301L);

        assertNotNull(response);
        assertEquals(AppointmentStatus.CONFIRMED, response.getStatus());
        assertEquals(AppointmentStatus.CONFIRMED, pendingAppt.getStatus());
        verify(appointmentRepository, times(1)).save(pendingAppt);
    }
}
