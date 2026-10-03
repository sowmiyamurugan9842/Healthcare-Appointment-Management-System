package com.example.healthcareappointmentmanagementsystem.controller;

import com.example.healthcareappointmentmanagementsystem.dto.request.ChatbotRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.ChatbotResponse;
import com.example.healthcareappointmentmanagementsystem.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing REST endpoints for AI Chatbot interactions.
 */
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * Constructor injection. Spring Boot automatically injects ChatbotService.
     */
    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    /**
     * Endpoint to send a query or action to the AI Chatbot.
     * Maps to POST /api/chatbot/message.
     * Access: Authenticated users (PATIENT, DOCTOR, ADMIN).
     *
     * @param request ChatbotRequest DTO with user message and conversation context
     * @return ChatbotResponse DTO with response message and interactive payload data
     */
    @PostMapping("/message")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<ChatbotResponse> sendMessage(@Valid @RequestBody ChatbotRequest request) {
        ChatbotResponse response = chatbotService.processMessage(request);
        return ResponseEntity.ok(response);
    }
}
