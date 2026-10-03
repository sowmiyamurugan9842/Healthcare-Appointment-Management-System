package com.example.healthcareappointmentmanagementsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HealthcareAppointmentManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthcareAppointmentManagementSystemApplication.class, args);
	}

}
