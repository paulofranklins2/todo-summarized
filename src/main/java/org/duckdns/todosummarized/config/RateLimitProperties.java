package org.duckdns.todosummarized.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for rate limiting.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {

    /**
     * Whether rate limiting is enabled.
     */
    private boolean enabled = true;

    /**
     * Rate limit configuration for the AI summary endpoint.
     */
    private EndpointLimit aiSummary = new EndpointLimit(10, 60);

    /**
     * Rate limit configuration for the daily summary endpoint.
     */
    private EndpointLimit dailySummary = new EndpointLimit(30, 60);

    /**
     * Configuration for a specific endpoint's rate limit.
     */
    @Data
    public static class EndpointLimit {
        /**
         * Maximum number of requests allowed in the time window.
         */
        private int maxRequests;

        /**
         * Time window in seconds.
         */
        private int windowSeconds;

        public EndpointLimit(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }
}

