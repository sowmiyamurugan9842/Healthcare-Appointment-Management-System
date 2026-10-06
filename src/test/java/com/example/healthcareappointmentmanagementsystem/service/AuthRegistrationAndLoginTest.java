package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.request.LoginRequest;
import com.example.healthcareappointmentmanagementsystem.dto.request.RegisterRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.UserResponse;
import com.example.healthcareappointmentmanagementsystem.entity.BloodGroup;
import com.example.healthcareappointmentmanagementsystem.entity.Gender;
import com.example.healthcareappointmentmanagementsystem.entity.Patient;
import com.example.healthcareappointmentmanagementsystem.entity.Role;
import com.example.healthcareappointmentmanagementsystem.entity.User;
import com.example.healthcareappointmentmanagementsystem.exception.DuplicateResourceException;
import com.example.healthcareappointmentmanagementsystem.exception.UnauthorizedException;
import com.example.healthcareappointmentmanagementsystem.mapper.UserMapper;
import com.example.healthcareappointmentmanagementsystem.repository.PatientRepository;
import com.example.healthcareappointmentmanagementsystem.repository.UserRepository;
import com.example.healthcareappointmentmanagementsystem.security.CustomUserDetailsService;
import com.example.healthcareappointmentmanagementsystem.security.JwtService;
import com.example.healthcareappointmentmanagementsystem.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthRegistrationAndLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthServiceImpl authService;
    private CustomUserDetailsService customUserDetailsService;

    private User sampleUser;
    private Patient samplePatient;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                patientRepository,
                userMapper,
                passwordEncoder,
                authenticationManager,
                jwtService
        );

        customUserDetailsService = new CustomUserDetailsService(userRepository);

        sampleUser = User.builder()
                .id(25L)
                .firstName("Sarah")
                .lastName("Jenkins")
                .email("sarah.jenkins@careportal.com")
                .password("encodedPassword123")
                .phoneNumber("9876543210")
                .role(Role.PATIENT)
                .enabled(true)
                .build();

        samplePatient = Patient.builder()
                .id(12L)
                .user(sampleUser)
                .gender(Gender.FEMALE)
                .bloodGroup(BloodGroup.O_POSITIVE)
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .address("123 Healthcare Blvd")
                .emergencyContact("9876543210")
                .build();
    }

    @Test
    @DisplayName("Patient Registration: Automatically creates User and Patient profile returning both IDs")
    void register_asPatient_createsUserAndPatientProfile_returnsBothIds() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Sarah")
                .lastName("Jenkins")
                .email("sarah.jenkins@careportal.com")
                .password("plainPassword123")
                .phoneNumber("9876543210")
                .role(Role.PATIENT)
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender(Gender.FEMALE)
                .bloodGroup(BloodGroup.O_POSITIVE)
                .address("123 Healthcare Blvd")
                .emergencyContact("9876543210")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(sampleUser);
        when(passwordEncoder.encode("plainPassword123")).thenReturn("encodedPassword123");
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(patientRepository.save(any(Patient.class))).thenReturn(samplePatient);

        UserResponse mappedUserResponse = UserResponse.builder()
                .id(25L)
                .firstName("Sarah")
                .lastName("Jenkins")
                .email("sarah.jenkins@careportal.com")
                .role(Role.PATIENT)
                .enabled(true)
                .build();
        when(userMapper.toResponse(sampleUser)).thenReturn(mappedUserResponse);

        UserResponse actual = authService.register(request);

        assertNotNull(actual);
        assertEquals(25L, actual.getId());
        assertEquals(12L, actual.getPatientProfileId());
        assertEquals(Role.PATIENT, actual.getRole());

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        Patient capturedPatient = patientCaptor.getValue();
        assertEquals(sampleUser, capturedPatient.getUser());
        assertEquals(LocalDate.of(1995, 5, 15), capturedPatient.getDateOfBirth());
        assertEquals(Gender.FEMALE, capturedPatient.getGender());
        assertEquals(BloodGroup.O_POSITIVE, capturedPatient.getBloodGroup());
    }

    @Test
    @DisplayName("Doctor/Admin Registration: Creates User without Patient profile")
    void register_asDoctor_createsUserWithoutPatientProfile() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Smith")
                .email("dr.smith@careportal.com")
                .password("plainPassword123")
                .phoneNumber("9876543210")
                .role(Role.DOCTOR)
                .build();

        User doctorUser = User.builder()
                .id(30L)
                .firstName("John")
                .lastName("Smith")
                .email("dr.smith@careportal.com")
                .password("encodedPassword123")
                .role(Role.DOCTOR)
                .enabled(true)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(doctorUser);
        when(passwordEncoder.encode("plainPassword123")).thenReturn("encodedPassword123");
        when(userRepository.save(doctorUser)).thenReturn(doctorUser);

        UserResponse mappedResponse = UserResponse.builder()
                .id(30L)
                .firstName("John")
                .lastName("Smith")
                .email("dr.smith@careportal.com")
                .role(Role.DOCTOR)
                .enabled(true)
                .build();
        when(userMapper.toResponse(doctorUser)).thenReturn(mappedResponse);

        UserResponse actual = authService.register(request);

        assertNotNull(actual);
        assertEquals(30L, actual.getId());
        assertNull(actual.getPatientProfileId());
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registration: Throws DuplicateResourceException when email already exists")
    void register_duplicateEmail_throwsDuplicateResourceException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("duplicate@careportal.com")
                .password("password123")
                .role(Role.PATIENT)
                .build();

        when(userRepository.existsByEmail("duplicate@careportal.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login with Email: Authenticates via email and returns JWT token")
    void login_withEmail_succeeds() {
        LoginRequest request = LoginRequest.builder()
                .email("sarah.jenkins@careportal.com")
                .password("plainPassword123")
                .build();

        when(userRepository.findByEmail("sarah.jenkins@careportal.com")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(any(), any(UserDetails.class))).thenReturn("jwt.token.email");

        String token = authService.login(request);

        assertEquals("jwt.token.email", token);
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("sarah.jenkins@careportal.com", "plainPassword123")
        );
    }

    @Test
    @DisplayName("Login with User ID: Finds user by numeric ID and authenticates")
    void login_withUserId_succeeds() {
        LoginRequest request = LoginRequest.builder()
                .email("25") // User entered User ID into login field
                .password("plainPassword123")
                .build();

        when(userRepository.findById(25L)).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(any(), any(UserDetails.class))).thenReturn("jwt.token.userid");

        String token = authService.login(request);

        assertEquals("jwt.token.userid", token);
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("sarah.jenkins@careportal.com", "plainPassword123")
        );
    }

    @Test
    @DisplayName("Login with invalid password: Throws UnauthorizedException")
    void login_invalidPassword_throwsUnauthorizedException() {
        LoginRequest request = LoginRequest.builder()
                .email("sarah.jenkins@careportal.com")
                .password("wrongPassword")
                .build();

        when(userRepository.findByEmail("sarah.jenkins@careportal.com")).thenReturn(Optional.of(sampleUser));
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login with non-existent User ID: Throws UnauthorizedException")
    void login_nonExistentUserId_throwsUnauthorizedException() {
        LoginRequest request = LoginRequest.builder()
                .email("999") // Non-existent user ID
                .password("password123")
                .build();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login with deactivated account: Throws UnauthorizedException")
    void login_deactivatedAccount_throwsUnauthorizedException() {
        sampleUser.setEnabled(false);

        LoginRequest request = LoginRequest.builder()
                .email("sarah.jenkins@careportal.com")
                .password("plainPassword123")
                .build();

        when(userRepository.findByEmail("sarah.jenkins@careportal.com")).thenReturn(Optional.of(sampleUser));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("CustomUserDetailsService: Loads UserDetails by numeric User ID")
    void customUserDetailsService_loadByUserId_succeeds() {
        when(userRepository.findById(25L)).thenReturn(Optional.of(sampleUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("25");

        assertNotNull(userDetails);
        assertEquals("sarah.jenkins@careportal.com", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT")));
    }

    @Test
    @DisplayName("CustomUserDetailsService: Loads UserDetails by Email")
    void customUserDetailsService_loadByEmail_succeeds() {
        when(userRepository.findByEmail("sarah.jenkins@careportal.com")).thenReturn(Optional.of(sampleUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("sarah.jenkins@careportal.com");

        assertNotNull(userDetails);
        assertEquals("sarah.jenkins@careportal.com", userDetails.getUsername());
    }

    @Test
    @DisplayName("CustomUserDetailsService: Throws UsernameNotFoundException for invalid identifier")
    void customUserDetailsService_notFound_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("unknown@careportal.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> customUserDetailsService.loadUserByUsername("unknown@careportal.com"));
    }
}
