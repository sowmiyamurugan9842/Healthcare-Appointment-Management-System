package com.example.healthcareappointmentmanagementsystem.controller;

import com.example.healthcareappointmentmanagementsystem.dto.request.WaitlistRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.AppointmentResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.WaitlistResponse;
import com.example.healthcareappointmentmanagementsystem.service.WaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller providing endpoints for Waitlist management, patient queues, and slot offer confirmations.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Waitlist Management", description = "Endpoints for managing patient appointment waitlists and slot reallocations")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @Operation(summary = "Join Waitlist", description = "Adds the authenticated patient to a doctor's waitlist for a fully booked date.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Successfully joined waitlist"),
            @ApiResponse(responseCode = "400", description = "Invalid request or duplicate waitlist entry"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Doctor or Patient profile not found")
    })
    @PostMapping("/waitlist")
    public ResponseEntity<WaitlistResponse> joinWaitlist(@Valid @RequestBody WaitlistRequest request) {
        WaitlistResponse response = waitlistService.joinWaitlist(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get My Waitlist", description = "Retrieves all waitlist entries for the currently authenticated patient.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved patient waitlist"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping("/waitlist/my")
    public ResponseEntity<List<WaitlistResponse>> getMyWaitlist() {
        List<WaitlistResponse> responses = waitlistService.getMyWaitlist();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Cancel Waitlist Entry", description = "Leaves or cancels an active waitlist entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully cancelled waitlist entry"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel already booked/expired entry"),
            @ApiResponse(responseCode = "404", description = "Waitlist entry not found")
    })
    @DeleteMapping("/waitlist/{id}")
    public ResponseEntity<WaitlistResponse> cancelWaitlistEntry(@PathVariable Long id) {
        WaitlistResponse response = waitlistService.cancelWaitlistEntry(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Doctor Waitlist", description = "Retrieves waitlist entries for a specific doctor (Doctor/Admin access).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved doctor waitlist"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access to another doctor's waitlist"),
            @ApiResponse(responseCode = "404", description = "Doctor not found")
    })
    @GetMapping(value = {"/doctors/{doctorId}/waitlist", "/waitlist/doctor/{doctorId}"})
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<List<WaitlistResponse>> getWaitlistByDoctor(@PathVariable Long doctorId) {
        List<WaitlistResponse> responses = waitlistService.getWaitlistByDoctor(doctorId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Confirm Offered Slot", description = "Confirms an offered slot and converts the waitlist entry into an official appointment.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully confirmed offer and scheduled appointment"),
            @ApiResponse(responseCode = "400", description = "Offer expired or invalid state"),
            @ApiResponse(responseCode = "404", description = "Waitlist entry not found")
    })
    @PostMapping("/waitlist/{id}/confirm")
    public ResponseEntity<AppointmentResponse> confirmOfferedSlot(@PathVariable Long id) {
        AppointmentResponse response = waitlistService.confirmOfferedSlot(id);
        return ResponseEntity.ok(response);
    }
}
