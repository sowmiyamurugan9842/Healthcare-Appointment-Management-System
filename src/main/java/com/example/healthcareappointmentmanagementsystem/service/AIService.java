package com.example.healthcareappointmentmanagementsystem.service;

/**
 * Service interface for AI & LLM processing.
 * Generates natural language responses based on context and prompts.
 */
public interface AIService {

    /**
     * Generates a conversational response given system instructions and user message.
     *
     * @param systemPrompt System instructions and clinical context constraints
     * @param userMessage  User input query or context data
     * @return Generated AI response text
     */
    String generateResponse(String systemPrompt, String userMessage);
}
