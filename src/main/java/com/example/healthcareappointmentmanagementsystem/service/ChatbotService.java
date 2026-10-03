package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.request.ChatbotRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.ChatbotResponse;

/**
 * Service interface handling AI Chatbot interactions for authenticated patients.
 */
public interface ChatbotService {

    /**
     * Processes an incoming user query/action and executes corresponding healthcare tool workflows.
     *
     * @param request ChatbotRequest DTO with user message and multi-turn context
     * @return ChatbotResponse DTO containing message text and interactive payloads
     */
    ChatbotResponse processMessage(ChatbotRequest request);
}
