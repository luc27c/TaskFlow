package com.automation.taskplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Service
public class AiSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiSummaryService.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    @Value("${openai.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (isEnabled()) {
            log.info("OpenAI service initialized");
        } else {
            log.warn("OpenAI API key not configured - AI summaries will be disabled");
        }
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public String summarizeEmail(String from, String subject, String snippet) {
        if (!isEnabled()) {
            return null;
        }

        try {
            String prompt = String.format(
                "Summarize this email in 1 concise sentence. If there's an action item or deadline, mention it.\n\n" +
                "From: %s\n" +
                "Subject: %s\n" +
                "Preview: %s",
                from, subject, snippet
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                    Map.of("role", "system", "content", "You are a concise email summarizer. Respond with only the summary, no extra text."),
                    Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 100,
                "temperature", 0.3
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_API_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String summary = root.path("choices").path(0).path("message").path("content").asText().trim();
                log.debug("Generated summary for email '{}': {}", subject, summary);
                return summary;
            }

        } catch (Exception e) {
            log.error("Failed to generate AI summary for email '{}': {}", subject, e.getMessage());
        }
        return null;
    }

    public record EmailInfo(String from, String subject, String snippet) {}
}
