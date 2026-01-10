package org.duckdns.todosummarized.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.duckdns.todosummarized.config.GeminiProperties;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Adapter service for AI-powered summary generation using Google Gemini API.
 * Handles API communication, error handling, and fallback scenarios.
 * Uses a shared HttpClient instance for connection pooling and performance.
 */
@Slf4j
@Service
public class GeminiSummaryAdapter extends BaseAiSummaryAdapter {

    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private final GeminiProperties geminiProperties;

    public GeminiSummaryAdapter(GeminiProperties geminiProperties, ObjectMapper objectMapper,
                                 AiSummaryMessageBuilder messageBuilder) {
        super(objectMapper, messageBuilder);
        this.geminiProperties = geminiProperties;
    }

    @Override
    protected String getProviderName() {
        return "Gemini";
    }

    @Override
    public boolean isEnabled() {
        return geminiProperties.isEnabled();
    }

    @Override
    public String getModel() {
        return geminiProperties.getModel();
    }

    @Override
    protected String getApiKey() {
        return geminiProperties.getApiKey();
    }

    @Override
    protected int getTimeoutSeconds() {
        return geminiProperties.getTimeoutSeconds();
    }

    @Override
    protected String callApi(String systemPrompt, String userMessage) throws Exception {
        Duration requestTimeout = Duration.ofSeconds(geminiProperties.getTimeoutSeconds());

        // Gemini API uses a different structure than OpenAI
        // Combine system prompt and user message into content parts
        String combinedPrompt = messageBuilder.combinedPrompt(systemPrompt, userMessage);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", combinedPrompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "maxOutputTokens", geminiProperties.getMaxTokens(),
                        "temperature", geminiProperties.getTemperature()
                )
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // Gemini API URL format: https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={API_KEY}
        String apiUrl = GEMINI_API_BASE_URL + geminiProperties.getModel() + ":generateContent?key=" + geminiProperties.getApiKey();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", CONTENT_TYPE_JSON)
                .timeout(requestTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Gemini API error: status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("Gemini API returned status " + response.statusCode());
        }

        return parseGeminiResponse(response.body());
    }

    /**
     * Parses the Gemini API response and extracts the generated content.
     *
     * @param responseBody the raw JSON response body
     * @return the extracted content text
     * @throws Exception if response format is unexpected
     */
    private String parseGeminiResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.get("candidates");
        if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
            JsonNode content = candidates.get(0).get("content");
            if (content != null) {
                JsonNode parts = content.get("parts");
                if (parts != null && parts.isArray() && !parts.isEmpty()) {
                    JsonNode text = parts.get(0).get("text");
                    if (text != null) {
                        return text.asText();
                    }
                }
            }
        }
        throw new RuntimeException("Unexpected Gemini response format");
    }
}
