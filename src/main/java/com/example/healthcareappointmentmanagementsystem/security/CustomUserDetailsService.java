package com.example.healthcareappointmentmanagementsystem.security;

import com.example.healthcareappointmentmanagementsystem.repository.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Custom implementation of Spring Security's UserDetailsService interface.
 * Bridges Spring Security's authentication manager with our database.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Constructor injection. Spring Boot automatically injects UserRepository.
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        com.example.healthcareappointmentmanagementsystem.entity.User user = null;
        if (identifier != null && identifier.trim().matches("^\\d+$")) {
            try {
                Long userId = Long.parseLong(identifier.trim());
                user = userRepository.findById(userId).orElse(null);
            } catch (NumberFormatException ignored) {}
        }
        if (user == null) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + identifier));
        }

        // 2. Check if the user account is enabled (not deactivated)
        if (user.getEnabled() == null || !user.getEnabled()) {
            throw new DisabledException("User account is disabled: " + identifier);
        }

        // 3. Build and return Spring Security's UserDetails representation
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
