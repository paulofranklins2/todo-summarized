package org.duckdns.todosummarized.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.duckdns.todosummarized.domains.enums.SummaryType;
import org.duckdns.todosummarized.dto.DailySummaryDTO;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

/**
 * Abstract base class for AI summary adapters.
 * Provides common functionality for HTTP client lifecycle management,
 * validation, and summary generation flow.
 */
@Slf4j
public abstract class BaseAiSummaryAdapter {

    protected final ObjectMapper objectMapper;
    protected final AiSummaryMessageBuilder messageBuilder;
    protected HttpClient httpClient;

    protected BaseAiSummaryAdapter(ObjectMapper objectMapper, AiSummaryMessageBuilder messageBuilder) {
        this.objectMapper = objectMapper;
        this.messageBuilder = messageBuilder;
    }

    /**
     * Initializes the shared HttpClient after dependency injection.
     * Subclasses should call this in their @PostConstruct method.
     */
    @PostConstruct
    public void initHttpClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(getTimeoutSeconds()))
                .build();
        log.info("{} adapter initialized with model: {}, enabled: {}",
                getProviderName(), getModel(), isEnabled());
    }

    /**
     * Closes the HttpClient when the bean is destroyed.
     */
    @PreDestroy
    public void destroyHttpClient() {
        if (httpClient != null) {
            httpClient.close();
            log.info("{} adapter HttpClient closed", getProviderName());
        }
    }

    /**
     * Generates an AI summary for the given metrics using the specified summary type.
     * Returns empty Optional if AI is disabled or an error occurs.
     *
     * @param metrics     the daily summary metrics to summarize
     * @param summaryType the type of summary to generate
     * @return Optional containing the generated summary, or empty if unavailable
     */
    public Optional<String> generateSummary(DailySummaryDTO metrics, SummaryType summaryType) {
        if (!isEnabled()) {
            log.info("{} AI summary is disabled by configuration", getProviderName());
            return Optional.empty();
        }

        if (!isApiKeyConfigured()) {
            log.warn("{} API key is not configured", getProviderName());
            return Optional.empty();
        }

        try {
            String userMessage = messageBuilder.buildUserMessage(metrics);
            String response = callApi(summaryType.getPrompt(), userMessage);
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.error("Failed to generate {} AI summary: {}", getProviderName(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Returns the reason why this AI provider is unavailable.
     *
     * @return description of why AI cannot be used
     */
    public String getUnavailableReason() {
        if (!isEnabled()) {
            return getProviderName() + " AI summary feature is disabled";
        }
        if (!isApiKeyConfigured()) {
            return getProviderName() + " API key is not configured";
        }
        return getProviderName() + " AI service encountered an error";
    }

    /**
     * Checks if the API key is properly configured.
     *
     * @return true if API key is present and not blank
     */
    protected boolean isApiKeyConfigured() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    // Abstract methods to be implemented by specific providers

    /**
     * Returns the provider name for logging purposes.
     */
    protected abstract String getProviderName();

    /**
     * Checks if this AI provider is enabled.
     */
    public abstract boolean isEnabled();

    /**
     * Gets the configured model name.
     */
    public abstract String getModel();

    /**
     * Gets the configured API key.
     */
    protected abstract String getApiKey();

    /**
     * Gets the configured timeout in seconds.
     */
    protected abstract int getTimeoutSeconds();

    /**
     * Calls the AI provider's API with the given prompts.
     *
     * @param systemPrompt the system prompt for context
     * @param userMessage  the user message with metrics
     * @return the AI-generated response content
     * @throws Exception if API call fails
     */
    protected abstract String callApi(String systemPrompt, String userMessage) throws Exception;
}

