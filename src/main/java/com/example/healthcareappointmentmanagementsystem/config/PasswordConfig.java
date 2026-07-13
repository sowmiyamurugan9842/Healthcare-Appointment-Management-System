package com.example.healthcareappointmentmanagementsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration class exposing the password hashing encoder bean.
 * Separated from general security config to avoid dependency cycles during startup.
 */
@Configuration
public class PasswordConfig {

    /**
     * Exposes BCryptPasswordEncoder as a Spring Bean.
     * This encoder is injected into AuthServiceImpl to hash user passwords before saving.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
