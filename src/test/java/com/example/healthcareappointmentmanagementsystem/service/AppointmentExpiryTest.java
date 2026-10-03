package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.entity.*;
import com.example.healthcareappointmentmanagementsystem.mapper.AppointmentMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DoctorRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PatientRepository;
import com.example.healthcareappointmentmanagementsystem.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentExpiryTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    private AppointmentServiceImpl appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentServiceImpl(
                appointmentRepository,
                doctorRepository,
                patientRepository,
                appointmentMapper
        );
    }

    @Test
    @DisplayName("Case 1: Appointment = yesterday, Status = CONFIRMED -> Expected = EXPIRED")
    void testCase1_YesterdayConfirmed_Expires() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Appointment appt = Appointment.builder()
                .id(1L)
                .appointmentDate(yesterday)
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.CONFIRMED)
                .reasonForVisit("Cardiology Follow-up")
                .build();

        when(appointmentRepository.findByAppointmentDateBeforeAndStatusIn(eq(LocalDate.now()), any()))
                .thenReturn(Collections.singletonList(appt));

        int expiredCount = appointmentService.expireOverdueAppointments();

        assertEquals(1, expiredCount);
        assertEquals(AppointmentStatus.EXPIRED, appt.getStatus());
        verify(appointmentRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Case 2: Appointment = yesterday, Status = PENDING -> Expected = EXPIRED")
    void testCase2_YesterdayPending_Expires() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Appointment appt = Appointment.builder()
                .id(2L)
                .appointmentDate(yesterday)
                .appointmentTime(LocalTime.of(14, 30))
                .status(AppointmentStatus.PENDING)
                .reasonForVisit("General Consultation")
                .build();

        when(appointmentRepository.findByAppointmentDateBeforeAndStatusIn(eq(LocalDate.now()), any()))
                .thenReturn(Collections.singletonList(appt));

        int expiredCount = appointmentService.expireOverdueAppointments();

        assertEquals(1, expiredCount);
        assertEquals(AppointmentStatus.EXPIRED, appt.getStatus());
        verify(appointmentRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Case 3: Appointment = yesterday, Status = COMPLETED -> Expected = COMPLETED (Never changes to EXPIRED)")
    void testCase3_YesterdayCompleted_StaysCompleted() {
        // Expirable query only queries for statuses PENDING and CONFIRMED.
        // Completed appointments are never queried or mutated.
        when(appointmentRepository.findByAppointmentDateBeforeAndStatusIn(
                eq(LocalDate.now()),
                eq(Arrays.asList(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED))
        )).thenReturn(Collections.emptyList());

        int expiredCount = appointmentService.expireOverdueAppointments();

        assertEquals(0, expiredCount);
        verify(appointmentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Case 4: Appointment = yesterday, Status = CANCELLED -> Expected = CANCELLED (Never changes to EXPIRED)")
    void testCase4_YesterdayCancelled_StaysCancelled() {
        // Cancelled appointments are excluded from the expiry query.
        when(appointmentRepository.findByAppointmentDateBeforeAndStatusIn(
                eq(LocalDate.now()),
                eq(Arrays.asList(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED))
        )).thenReturn(Collections.emptyList());

        int expiredCount = appointmentService.expireOverdueAppointments();

        assertEquals(0, expiredCount);
        verify(appointmentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Case 5: Appointment = today, Status = CONFIRMED -> Expected = CONFIRMED (Stays active today)")
    void testCase5_TodayConfirmed_StaysConfirmed() {
        // appointmentDate is today, so appointmentDate < LocalDate.now() is FALSE.
        when(appointmentRepository.findByAppointmentDateBeforeAndStatusIn(
                eq(LocalDate.now()),
                eq(Arrays.asList(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED))
        )).thenReturn(Collections.emptyList());

        int expiredCount = appointmentService.expireOverdueAppointments();

        assertEquals(0, expiredCount);
        verify(appointmentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Case 6: Appointment = tomorrow, Status = CONFIRMED -> Expected = CONFIRMED (Stays active for future)")
    void testCase6_TomorrowConfirmed_StaysConfirmed() {
        // Future appointment is not before today.
        when(appointmentRepository.findByAppointmentDateBeforeAndStatusIn(
                eq(LocalDate.now()),
                eq(Arrays.asList(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED))
        )).thenReturn(Collections.emptyList());

        int expiredCount = appointmentService.expireOverdueAppointments();

        assertEquals(0, expiredCount);
        verify(appointmentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Case 7: Appointment = today, appointment time already passed, Status = CONFIRMED -> Expected = STILL CONFIRMED (Must NOT become EXPIRED until next day)")
    void testCase7_TodayPastTimeConfirmed_StaysConfirmedUntilNextDay() {
        // Even if appointmentTime is in the past (e.g., 08:00 AM while current time is 11:30 AM),
        // because appointmentDate == today, findByAppointmentDateBefore(LocalDate.now(), ...) returns empty.
        when(appointmentRepository.findByAppointmentDateBeforeAndStatusIn(
                eq(LocalDate.now()),
                eq(Arrays.asList(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED))
        )).thenReturn(Collections.emptyList());

        int expiredCount = appointmentService.expireOverdueAppointments();

        assertEquals(0, expiredCount);
        verify(appointmentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Batch Expiration: Multiple overdue appointments are all transitioned to EXPIRED")
    void testBatchExpiration() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate twoDaysAgo = LocalDate.now().minusDays(2);

        Appointment appt1 = Appointment.builder()
                .id(101L)
                .appointmentDate(yesterday)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        Appointment appt2 = Appointment.builder()
                .id(102L)
                .appointmentDate(twoDaysAgo)
                .status(AppointmentStatus.PENDING)
                .build();

        when(appointmentRepository.findByAppointmentDateBeforeAndStatusIn(eq(LocalDate.now()), any()))
                .thenReturn(Arrays.asList(appt1, appt2));

        int expiredCount = appointmentService.expireOverdueAppointments();

        assertEquals(2, expiredCount);
        assertEquals(AppointmentStatus.EXPIRED, appt1.getStatus());
        assertEquals(AppointmentStatus.EXPIRED, appt2.getStatus());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Appointment>> captor = ArgumentCaptor.forClass(List.class);
        verify(appointmentRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
    }
}
