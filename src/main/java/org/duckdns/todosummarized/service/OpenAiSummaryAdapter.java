package org.duckdns.todosummarized.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.duckdns.todosummarized.config.OpenAiProperties;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Adapter service for AI-powered summary generation using OpenAI API.
 * Handles API communication, error handling, and fallback scenarios.
 * Uses a shared HttpClient instance for connection pooling and performance.
 */
@Slf4j
@Service
public class OpenAiSummaryAdapter extends BaseAiSummaryAdapter {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String AUTH_HEADER_PREFIX = "Bearer ";

    private final OpenAiProperties openAiProperties;

    public OpenAiSummaryAdapter(OpenAiProperties openAiProperties, ObjectMapper objectMapper,
                                AiSummaryMessageBuilder messageBuilder) {
        super(objectMapper, messageBuilder);
        this.openAiProperties = openAiProperties;
    }

    @Override
    protected String getProviderName() {
        return "OpenAI";
    }

    @Override
    public boolean isEnabled() {
        return openAiProperties.isEnabled();
    }

    @Override
    public String getModel() {
        return openAiProperties.getModel();
    }

    @Override
    protected String getApiKey() {
        return openAiProperties.getApiKey();
    }

    @Override
    protected int getTimeoutSeconds() {
        return openAiProperties.getTimeoutSeconds();
    }

    @Override
    protected String callApi(String systemPrompt, String userMessage) throws Exception {
        Duration requestTimeout = Duration.ofSeconds(openAiProperties.getTimeoutSeconds());

        Map<String, Object> requestBody = Map.of(
                "model", openAiProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", openAiProperties.getMaxTokens(),
                "temperature", openAiProperties.getTemperature()
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Content-Type", CONTENT_TYPE_JSON)
                .header("Authorization", AUTH_HEADER_PREFIX + openAiProperties.getApiKey())
                .timeout(requestTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("OpenAI API error: status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("OpenAI API returned status " + response.statusCode());
        }

        return parseOpenAiResponse(response.body());
    }

    /**
     * Parses the OpenAI API response and extracts the generated content.
     */
    private String parseOpenAiResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && !choices.isEmpty()) {
            JsonNode message = choices.get(0).get("message");
            if (message != null && message.has("content")) {
                return message.get("content").asText();
            }
        }
        throw new RuntimeException("Unexpected OpenAI response format");
    }
}

