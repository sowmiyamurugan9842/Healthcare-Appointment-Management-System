package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.request.LoginRequest;
import com.example.healthcareappointmentmanagementsystem.dto.request.RegisterRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.UserResponse;

/**
 * Service interface handling authentication and authorization logic.
 */
public interface AuthService {

    /**
     * Registers a new user account (Admin, Doctor, Patient) in the system.
     *
     * @param request RegisterRequest DTO containing account details
     * @return UserResponse DTO containing registered profile info
     */
    UserResponse register(RegisterRequest request);

    /**
     * Authenticates a user with email and password, returning a JWT token on success.
     *
     * @param request LoginRequest DTO containing credentials
     * @return a JWT bearer token string on successful authentication
     */
    String login(LoginRequest request);
}
