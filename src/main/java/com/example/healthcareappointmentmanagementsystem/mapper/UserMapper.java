package com.example.healthcareappointmentmanagementsystem.mapper;

import com.example.healthcareappointmentmanagementsystem.dto.request.RegisterRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.UserResponse;
import com.example.healthcareappointmentmanagementsystem.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper class to convert between User entity and User DTOs.
 */
@Component
public class UserMapper {

    /**
     * Converts a User entity to a UserResponse DTO.
     *
     * @param user User entity
     * @return UserResponse DTO
     */
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .build();
    }

    /**
     * Converts a RegisterRequest DTO to a User entity.
     *
     * @param request RegisterRequest DTO
     * @return User entity
     */
    public User toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }

        return User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(request.getPassword()) // Note: Password hashing will be done in the Service layer
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .enabled(true) // Accounts are enabled by default upon registration
                .build();
    }
}
