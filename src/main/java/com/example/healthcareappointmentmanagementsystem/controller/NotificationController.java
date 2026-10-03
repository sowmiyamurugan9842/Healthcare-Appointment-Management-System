package com.example.healthcareappointmentmanagementsystem.controller;

import com.example.healthcareappointmentmanagementsystem.dto.response.NotificationResponse;
import com.example.healthcareappointmentmanagementsystem.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for patient in-app notifications and appointment reminders.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Retrieve all notifications for a specific patient.
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByPatient(@PathVariable Long patientId) {
        List<NotificationResponse> notifications = notificationService.getNotificationsByPatient(patientId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Retrieve unread notification count for a specific patient.
     */
    @GetMapping("/patient/{patientId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long patientId) {
        long count = notificationService.getUnreadCount(patientId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * Mark a specific notification as read.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        NotificationResponse updated = notificationService.markAsRead(id);
        return ResponseEntity.ok(updated);
    }

    /**
     * Mark all notifications as read for a patient.
     */
    @PutMapping("/patient/{patientId}/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(@PathVariable Long patientId) {
        notificationService.markAllAsRead(patientId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
