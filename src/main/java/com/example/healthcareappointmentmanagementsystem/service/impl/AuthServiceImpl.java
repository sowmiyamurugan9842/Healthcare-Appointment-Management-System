package com.example.healthcareappointmentmanagementsystem.service.impl;

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
import com.example.healthcareappointmentmanagementsystem.security.JwtService;
import com.example.healthcareappointmentmanagementsystem.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Service implementation handling user registration and login logic.
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Constructor injection. Spring Boot automatically injects the required beans.
     */
    public AuthServiceImpl(UserRepository userRepository,
                           PatientRepository patientRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        // 1. Check if email already exists in the database
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered: " + request.getEmail());
        }

        // 2. Map the incoming DTO request to a JPA User entity
        User user = userMapper.toEntity(request);

        // 3. Encrypt the raw plaintext password before saving to the database
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 4. Save the user entity to the database
        User savedUser = userRepository.save(user);

        // 5. If registering as PATIENT, automatically create the linked Patient medical profile
        Long patientProfileId = null;
        if (savedUser.getRole() == Role.PATIENT) {
            LocalDate dob = request.getDateOfBirth() != null ? request.getDateOfBirth() : LocalDate.of(2000, 1, 1);
            Gender gender = request.getGender() != null ? request.getGender() : Gender.MALE;
            BloodGroup bloodGroup = request.getBloodGroup() != null ? request.getBloodGroup() : BloodGroup.A_POSITIVE;
            String address = (request.getAddress() != null && !request.getAddress().trim().isEmpty())
                    ? request.getAddress().trim()
                    : "Address Not Provided";
            String emergency = (request.getEmergencyContact() != null && !request.getEmergencyContact().trim().isEmpty())
                    ? request.getEmergencyContact().trim()
                    : (savedUser.getPhoneNumber() != null ? savedUser.getPhoneNumber() : "0000000000");

            Patient patient = Patient.builder()
                    .user(savedUser)
                    .dateOfBirth(dob)
                    .gender(gender)
                    .bloodGroup(bloodGroup)
                    .address(address)
                    .emergencyContact(emergency)
                    .allergies(request.getAllergies())
                    .medicalHistory(request.getMedicalHistory())
                    .build();

            Patient savedPatient = patientRepository.save(patient);
            patientProfileId = savedPatient.getId();
        }

        // 6. Convert the saved entity back to a lightweight UserResponse DTO with patientProfileId
        UserResponse response = userMapper.toResponse(savedUser);
        response.setPatientProfileId(patientProfileId);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public String login(LoginRequest request) {
        String identifier = request.getEmail() != null ? request.getEmail().trim() : "";
        if (identifier.isEmpty() && request.getUserId() != null) {
            identifier = String.valueOf(request.getUserId());
        }

        // 1. Determine user by ID if numeric identifier, or by email
        User user = null;
        if (identifier.matches("^\\d+$")) {
            try {
                Long userId = Long.parseLong(identifier);
                user = userRepository.findById(userId).orElse(null);
            } catch (NumberFormatException ignored) {}
        }
        if (user == null) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new UnauthorizedException("Invalid email/User ID or password"));
        }

        // 2. Authenticate credentials using AuthenticationManager with the registered email
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            // Throw generic unauthorized exception to mask password vs identifier checks
            throw new UnauthorizedException("Invalid email/User ID or password");
        }

        // 3. Verify that the account is enabled
        if (user.getEnabled() == null || !user.getEnabled()) {
            throw new UnauthorizedException("User account is deactivated");
        }

        // 4. Build Spring UserDetails object for the token generator
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        // 5. Generate and return the signed JWT token
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "ROLE_" + user.getRole().name());
        extraClaims.put("userId", user.getId());

        return jwtService.generateToken(extraClaims, userDetails);
    }
}

