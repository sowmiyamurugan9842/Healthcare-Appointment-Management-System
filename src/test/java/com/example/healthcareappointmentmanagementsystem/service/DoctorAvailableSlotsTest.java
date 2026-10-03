package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.response.DoctorAvailableSlotsResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Appointment;
import com.example.healthcareappointmentmanagementsystem.entity.AppointmentStatus;
import com.example.healthcareappointmentmanagementsystem.entity.Department;
import com.example.healthcareappointmentmanagementsystem.entity.Doctor;
import com.example.healthcareappointmentmanagementsystem.entity.Patient;
import com.example.healthcareappointmentmanagementsystem.entity.Role;
import com.example.healthcareappointmentmanagementsystem.entity.User;
import com.example.healthcareappointmentmanagementsystem.exception.BadRequestException;
import com.example.healthcareappointmentmanagementsystem.exception.ResourceNotFoundException;
import com.example.healthcareappointmentmanagementsystem.mapper.DoctorMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DepartmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DoctorRepository;
import com.example.healthcareappointmentmanagementsystem.repository.UserRepository;
import com.example.healthcareappointmentmanagementsystem.service.impl.DoctorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DoctorAvailableSlotsTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DoctorMapper doctorMapper;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private DoctorServiceImpl doctorService;

    private Doctor testDoctor;
    private Patient testPatient;

    @BeforeEach
    void setUp() {
        User doctorUser = User.builder()
                .id(10L)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@careportal.com")
                .role(Role.DOCTOR)
                .build();

        User patientUser = User.builder()
                .id(11L)
                .firstName("Sowmiya")
                .lastName("Murugan")
                .email("sowmiya@careportal.com")
                .role(Role.PATIENT)
                .build();

        Department dept = Department.builder()
                .id(1L)
                .departmentName("Cardiology")
                .build();

        testDoctor = Doctor.builder()
                .id(1L)
                .user(doctorUser)
                .department(dept)
                .qualification("MD, FACC")
                .specialization("Cardiology")
                .consultationFee(150.00)
                .availableFrom(LocalTime.of(9, 0))
                .availableTo(LocalTime.of(17, 0))
                .build();

        testPatient = Patient.builder()
                .id(5L)
                .user(patientUser)
                .build();
    }

    @Test
    @DisplayName("Should generate all 30-minute intervals between 09:00 and 17:00 when no appointments booked")
    void testGetAvailableSlots_FullWorkingHoursAvailable() {
        LocalDate targetDate = LocalDate.now().plusDays(2);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(testDoctor));
        when(appointmentRepository.findByDoctorAndAppointmentDate(testDoctor, targetDate)).thenReturn(new ArrayList<>());

        DoctorAvailableSlotsResponse response = doctorService.getAvailableSlots(1L, targetDate);

        assertNotNull(response);
        assertEquals(1L, response.getDoctorId());
        assertEquals(targetDate, response.getDate());
        assertEquals(16, response.getAvailableSlots().size());
        assertEquals("09:00", response.getAvailableSlots().get(0));
        assertEquals("09:30", response.getAvailableSlots().get(1));
        assertEquals("16:30", response.getAvailableSlots().get(15));
        assertTrue(response.getAvailableSlots().contains("10:00"));
        assertTrue(response.getAvailableSlots().contains("11:30"));
    }

    @Test
    @DisplayName("Should remove booked slots for PENDING, CONFIRMED, and COMPLETED appointments")
    void testGetAvailableSlots_RemovesBookedSlots() {
        LocalDate targetDate = LocalDate.now().plusDays(1);
        testDoctor.setAvailableFrom(LocalTime.of(9, 0));
        testDoctor.setAvailableTo(LocalTime.of(12, 0));

        Appointment appt1 = Appointment.builder()
                .id(101L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(targetDate)
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.CONFIRMED)
                .build();

        Appointment appt2 = Appointment.builder()
                .id(102L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(targetDate)
                .appointmentTime(LocalTime.of(11, 30))
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment appt3 = Appointment.builder()
                .id(103L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(targetDate)
                .appointmentTime(LocalTime.of(10, 30))
                .status(AppointmentStatus.COMPLETED)
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(testDoctor));
        when(appointmentRepository.findByDoctorAndAppointmentDate(testDoctor, targetDate))
                .thenReturn(Arrays.asList(appt1, appt2, appt3));

        DoctorAvailableSlotsResponse response = doctorService.getAvailableSlots(1L, targetDate);

        assertNotNull(response);
        assertEquals(1L, response.getDoctorId());
        assertEquals(targetDate, response.getDate());
        
        // Total slots between 9:00 and 12:00 = 6 slots (09:00, 09:30, 10:00, 10:30, 11:00, 11:30)
        // Booked: 10:00, 10:30, 11:30 -> Available: 09:00, 09:30, 11:00
        assertEquals(3, response.getAvailableSlots().size());
        assertEquals(List.of("09:00", "09:30", "11:00"), response.getAvailableSlots());
        assertFalse(response.getAvailableSlots().contains("10:00"));
        assertFalse(response.getAvailableSlots().contains("10:30"));
        assertFalse(response.getAvailableSlots().contains("11:30"));
    }

    @Test
    @DisplayName("Should not block slots for CANCELLED or EXPIRED appointments")
    void testGetAvailableSlots_AllowsCancelledAndExpiredSlots() {
        LocalDate targetDate = LocalDate.now().plusDays(3);
        testDoctor.setAvailableFrom(LocalTime.of(9, 0));
        testDoctor.setAvailableTo(LocalTime.of(11, 0));

        Appointment cancelledAppt = Appointment.builder()
                .id(201L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(targetDate)
                .appointmentTime(LocalTime.of(9, 30))
                .status(AppointmentStatus.CANCELLED)
                .build();

        Appointment expiredAppt = Appointment.builder()
                .id(202L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(targetDate)
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.EXPIRED)
                .build();

        Appointment confirmedAppt = Appointment.builder()
                .id(203L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(targetDate)
                .appointmentTime(LocalTime.of(10, 30))
                .status(AppointmentStatus.CONFIRMED)
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(testDoctor));
        when(appointmentRepository.findByDoctorAndAppointmentDate(testDoctor, targetDate))
                .thenReturn(Arrays.asList(cancelledAppt, expiredAppt, confirmedAppt));

        DoctorAvailableSlotsResponse response = doctorService.getAvailableSlots(1L, targetDate);

        assertNotNull(response);
        // Total slots for 9:00 to 11:00 = 09:00, 09:30, 10:00, 10:30 (4 slots)
        // 10:30 is CONFIRMED (blocked)
        // 09:30 (CANCELLED) and 10:00 (EXPIRED) remain available!
        assertEquals(3, response.getAvailableSlots().size());
        assertEquals(List.of("09:00", "09:30", "10:00"), response.getAvailableSlots());
        assertTrue(response.getAvailableSlots().contains("09:30"));
        assertTrue(response.getAvailableSlots().contains("10:00"));
        assertFalse(response.getAvailableSlots().contains("10:30"));
    }

    @Test
    @DisplayName("Should throw BadRequestException if date is in the past")
    void testGetAvailableSlots_PastDateThrowsBadRequestException() {
        LocalDate pastDate = LocalDate.now().minusDays(1);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                doctorService.getAvailableSlots(1L, pastDate)
        );

        assertTrue(ex.getMessage().contains("must be today or in the future"));
        verifyNoInteractions(doctorRepository);
    }

    @Test
    @DisplayName("Should throw BadRequestException if date is null")
    void testGetAvailableSlots_NullDateThrowsBadRequestException() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                doctorService.getAvailableSlots(1L, null)
        );

        assertTrue(ex.getMessage().contains("Appointment date is required"));
        verifyNoInteractions(doctorRepository);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException if doctor does not exist")
    void testGetAvailableSlots_DoctorNotFoundThrowsResourceNotFoundException() {
        LocalDate targetDate = LocalDate.now().plusDays(1);
        when(doctorRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                doctorService.getAvailableSlots(999L, targetDate)
        );

        assertTrue(ex.getMessage().contains("Doctor profile not found with ID: 999"));
    }

    @Test
    @DisplayName("Should return empty list if doctor working hours are missing or invalid")
    void testGetAvailableSlots_InvalidWorkingHoursReturnsEmptyList() {
        LocalDate targetDate = LocalDate.now().plusDays(1);
        testDoctor.setAvailableFrom(null);
        testDoctor.setAvailableTo(null);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(testDoctor));

        DoctorAvailableSlotsResponse response = doctorService.getAvailableSlots(1L, targetDate);

        assertNotNull(response);
        assertEquals(0, response.getAvailableSlots().size());
    }
}
