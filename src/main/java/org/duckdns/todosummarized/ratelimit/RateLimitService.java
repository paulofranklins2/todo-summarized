package org.duckdns.todosummarized.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.duckdns.todosummarized.config.RateLimitProperties;
import org.duckdns.todosummarized.config.RateLimitProperties.EndpointLimit;
import org.springframework.stereotype.Service;

/**
 * Service for managing rate limiting per user and endpoint.
 * Uses Caffeine cache for automatic memory management and expiration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String AI_SUMMARY_KEY = "ai-summary";
    private static final String DAILY_SUMMARY_KEY = "daily-summary";

    // Array indices for token bucket state
    private static final int TOKENS_INDEX = 0;
    private static final int LAST_REFILL_NANOS_INDEX = 1;

    private final RateLimitProperties properties;
    private final Cache<String, double[]> rateLimitCache;

    /**
     * Result of a consumption attempt.
     */
    public record ConsumptionResult(boolean consumed, long remainingTokens, long nanosToWait) {
    }

    @PostConstruct
    void init() {
        log.info("Rate limiting initialized - enabled: {}, AI summary: {}/{} req/sec, Daily summary: {}/{} req/sec",
                properties.isEnabled(),
                properties.getAiSummary().getMaxRequests(),
                properties.getAiSummary().getWindowSeconds(),
                properties.getDailySummary().getMaxRequests(),
                properties.getDailySummary().getWindowSeconds());
    }

    /**
     * Checks if rate limiting is enabled.
     */
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /**
     * Attempts to consume a token for the given user and endpoint.
     */
    public ConsumptionResult tryConsume(String userId, String endpointKey) {
        if (!properties.isEnabled()) {
            return new ConsumptionResult(true, Long.MAX_VALUE, 0);
        }

        String bucketKey = buildBucketKey(userId, endpointKey);
        EndpointLimit limit = getLimitConfig(endpointKey);
        long capacity = limit.getMaxRequests();
        long windowNanos = (long) limit.getWindowSeconds() * 1_000_000_000L;
        double refillRatePerNano = (double) capacity / windowNanos;

        final ConsumptionResult[] result = new ConsumptionResult[1];

        rateLimitCache.asMap().compute(bucketKey, (key, state) -> {
            long nowNanos = System.nanoTime();

            if (state == null) {
                // First request - initialize with full capacity minus 1 consumed token
                result[0] = new ConsumptionResult(true, capacity - 1, 0);
                return new double[]{capacity - 1, nowNanos};
            }

            double tokens = state[TOKENS_INDEX];
            double lastRefillNanos = state[LAST_REFILL_NANOS_INDEX];

            // Calculate elapsed time and tokens to add (greedy refill)
            long elapsedNanos = nowNanos - (long) lastRefillNanos;
            double tokensToAdd = elapsedNanos * refillRatePerNano;
            double newTokens = Math.min(capacity, tokens + tokensToAdd);

            if (newTokens >= 1.0) {
                // Consume 1 token
                double remaining = newTokens - 1.0;
                result[0] = new ConsumptionResult(true, (long) remaining, 0);
                return new double[]{remaining, nowNanos};
            } else {
                // Not enough tokens - calculate wait time
                double tokensNeeded = 1.0 - newTokens;
                long nanosToWait = (long) (tokensNeeded / refillRatePerNano);
                result[0] = new ConsumptionResult(false, 0, nanosToWait);
                // Update state with current tokens (time-based calculation)
                return new double[]{newTokens, nowNanos};
            }
        });

        return result[0];
    }

    /**
     * Gets the remaining tokens for a user and endpoint without consuming.
     */
    public long getRemainingTokens(String userId, String endpointKey) {
        if (!properties.isEnabled()) {
            return Long.MAX_VALUE;
        }

        String bucketKey = buildBucketKey(userId, endpointKey);
        double[] state = rateLimitCache.getIfPresent(bucketKey);

        EndpointLimit limit = getLimitConfig(endpointKey);
        long capacity = limit.getMaxRequests();

        if (state == null) {
            return capacity;
        }

        // Calculate current tokens based on elapsed time
        long windowNanos = (long) limit.getWindowSeconds() * 1_000_000_000L;
        double refillRatePerNano = (double) capacity / windowNanos;
        long elapsedNanos = System.nanoTime() - (long) state[LAST_REFILL_NANOS_INDEX];
        double tokensToAdd = elapsedNanos * refillRatePerNano;
        double currentTokens = Math.min(capacity, state[TOKENS_INDEX] + tokensToAdd);

        return (long) currentTokens;
    }

    /**
     * Builds the bucket cache key combining user ID and endpoint.
     */
    private String buildBucketKey(String userId, String endpointKey) {
        return userId + ":" + endpointKey;
    }

    /**
     * Gets the limit configuration for the given endpoint key.
     */
    private EndpointLimit getLimitConfig(String endpointKey) {
        return switch (endpointKey) {
            case AI_SUMMARY_KEY -> properties.getAiSummary();
            case DAILY_SUMMARY_KEY -> properties.getDailySummary();
            default -> {
                log.warn("Unknown rate limit key '{}', using AI summary defaults", endpointKey);
                yield properties.getAiSummary();
            }
        };
    }

    /**
     * Clears all cached bucket states. Useful for testing.
     */
    public void clearBuckets() {
        rateLimitCache.invalidateAll();
        log.info("Rate limit bucket states cleared");
    }

    /**
     * Returns the current number of cached bucket states. Useful for monitoring.
     */
    public long getBucketCount() {
        return rateLimitCache.estimatedSize();
    }
}

