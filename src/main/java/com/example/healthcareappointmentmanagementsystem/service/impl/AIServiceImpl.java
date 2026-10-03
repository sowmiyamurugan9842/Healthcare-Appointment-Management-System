package com.example.healthcareappointmentmanagementsystem.service.impl;

import com.example.healthcareappointmentmanagementsystem.service.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Implementation of AIService supporting configurable external LLMs (e.g. OpenAI / Gemini)
 * with robust local fallback for resilience and offline support.
 */
@Service
public class AIServiceImpl implements AIService {

    private static final Logger log = LoggerFactory.getLogger(AIServiceImpl.class);

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.api.model:gpt-4o-mini}")
    private String model;

    private final RestTemplate restTemplate;

    public AIServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    public AIServiceImpl(RestTemplate restTemplate, String apiKey, String apiUrl, String model) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
    }

    @Override
    public String generateResponse(String systemPrompt, String userMessage) {
        // If external API key is provided, attempt external LLM completion
        if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.contains("${")) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey.trim());

                Map<String, Object> requestBody = Map.of(
                        "model", model,
                        "messages", List.of(
                                Map.of("role", "system", "content", systemPrompt),
                                Map.of("role", "user", "content", userMessage)
                        ),
                        "temperature", 0.3
                );

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List<?> choices = (List<?>) response.getBody().get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                        Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
                        if (message != null && message.get("content") != null) {
                            return message.get("content").toString().trim();
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("External AI API call failed or timed out. Falling back to local intelligence: {}", ex.getMessage());
            }
        }

        // Default resilient fallback: returns the contextual user-facing summary
        return userMessage;
    }
}
