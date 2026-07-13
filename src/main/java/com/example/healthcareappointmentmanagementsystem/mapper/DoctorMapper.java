package com.example.healthcareappointmentmanagementsystem.mapper;

import com.example.healthcareappointmentmanagementsystem.dto.request.DoctorRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.DoctorResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Department;
import com.example.healthcareappointmentmanagementsystem.entity.Doctor;
import com.example.healthcareappointmentmanagementsystem.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper class to convert between Doctor entity and Doctor DTOs.
 */
@Component
public class DoctorMapper {

    /**
     * Converts a Doctor entity to a DoctorResponse DTO.
     * Maps user credentials and department name into a flat structure.
     *
     * @param doctor Doctor entity
     * @return DoctorResponse DTO
     */
    public DoctorResponse toResponse(Doctor doctor) {
        if (doctor == null) {
            return null;
        }

        User user = doctor.getUser();
        String fullName = "";
        String email = "";
        String phone = "";
        if (user != null) {
            fullName = "Dr. " + user.getFirstName() + " " + user.getLastName();
            email = user.getEmail();
            phone = user.getPhoneNumber();
        }

        Department dept = doctor.getDepartment();
        String departmentName = (dept != null) ? dept.getDepartmentName() : "";

        return DoctorResponse.builder()
                .id(doctor.getId())
                .fullName(fullName)
                .email(email)
                .phoneNumber(phone)
                .departmentName(departmentName)
                .qualification(doctor.getQualification())
                .specialization(doctor.getSpecialization())
                .experienceYears(doctor.getExperienceYears())
                .consultationFee(doctor.getConsultationFee())
                .availableFrom(doctor.getAvailableFrom())
                .availableTo(doctor.getAvailableTo())
                .build();
    }

    /**
     * Converts a DoctorRequest DTO to a Doctor entity using provided User and Department.
     *
     * @param request    DoctorRequest DTO
     * @param user       associated User entity
     * @param department associated Department entity
     * @return Doctor entity
     */
    public Doctor toEntity(DoctorRequest request, User user, Department department) {
        if (request == null) {
            return null;
        }

        return Doctor.builder()
                .user(user)
                .department(department)
                .qualification(request.getQualification())
                .specialization(request.getSpecialization())
                .experienceYears(request.getExperienceYears())
                .consultationFee(request.getConsultationFee())
                .availableFrom(request.getAvailableFrom())
                .availableTo(request.getAvailableTo())
                .build();
    }
}
