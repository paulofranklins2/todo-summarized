package org.duckdns.todosummarized.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Google Gemini API integration.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    /**
     * Gemini API key for authentication.
     */
    private String apiKey;

    /**
     * Gemini model to use.
     */
    private String model = "gemini-2.5-flash-lite";

    /**
     * Whether Gemini AI summary feature is enabled.
     */
    private boolean enabled;

    /**
     * Maximum tokens for AI response.
     */
    private int maxTokens;

    /**
     * Temperature for AI response (0.0 - 2.0).
     */
    private double temperature;

    /**
     * Request timeout in seconds.
     */
    private int timeoutSeconds;
}

