package com.example.healthcareappointmentmanagementsystem.service.impl;

import com.example.healthcareappointmentmanagementsystem.dto.request.LoginRequest;
import com.example.healthcareappointmentmanagementsystem.dto.request.RegisterRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.UserResponse;
import com.example.healthcareappointmentmanagementsystem.entity.User;
import com.example.healthcareappointmentmanagementsystem.exception.DuplicateResourceException;
import com.example.healthcareappointmentmanagementsystem.exception.UnauthorizedException;
import com.example.healthcareappointmentmanagementsystem.mapper.UserMapper;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Service implementation handling user registration and login logic.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Constructor injection. Spring Boot automatically injects the required beans.
     */
    public AuthServiceImpl(UserRepository userRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService) {
        this.userRepository = userRepository;
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

        // 4. Save the user entity to the MySQL database
        User savedUser = userRepository.save(user);

        // 5. Convert the saved entity back to a lightweight UserResponse DTO
        return userMapper.toResponse(savedUser);
    }

    @Override
    public String login(LoginRequest request) {
        // 1. Authenticate credentials using the AuthenticationManager
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            // Throw generic unauthorized exception to mask password vs email validation checks
            throw new UnauthorizedException("Invalid email or password");
        }

        // 2. Fetch the user profile from database
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

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

