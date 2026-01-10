package org.duckdns.todosummarized.config;

import lombok.Data;
import org.duckdns.todosummarized.domains.enums.AiProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Global configuration properties for AI summary feature.
 * Controls which provider to use and overall feature availability.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * The preferred AI provider to use.
     */
    private AiProvider provider;

    /**
     * Whether the AI summary feature is globally enabled.
     * When false, all AI providers are disabled regardless of their individual settings.
     */
    private boolean enabled;
}

