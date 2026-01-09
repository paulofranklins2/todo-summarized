package org.duckdns.todosummarized.ratelimit;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.duckdns.todosummarized.config.RateLimitProperties;
import org.duckdns.todosummarized.config.RateLimitProperties.EndpointLimit;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing rate limiting per user and endpoint.
 * Implements a lightweight Token Bucket algorithm without storing Bucket instances.
 * Thread-safe implementation using ConcurrentHashMap with atomic compute operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String AI_SUMMARY_KEY = "ai-summary";
    private static final String DAILY_SUMMARY_KEY = "daily-summary";

    private final RateLimitProperties properties;

    /**
     * Lightweight token bucket state storing only essential data.
     * Immutable record for thread-safety during ConcurrentHashMap operations.
     *
     * @param tokens          current available tokens
     * @param lastRefillNanos timestamp of last refill in nanoseconds
     */
    private record TokenBucketState(double tokens, long lastRefillNanos) {
    }

    /**
     * Result of a consumption attempt.
     *
     * @param consumed        whether the token was consumed
     * @param remainingTokens remaining tokens after the operation
     * @param nanosToWait     nanoseconds to wait before retry (0 if consumed)
     */
    public record ConsumptionResult(boolean consumed, long remainingTokens, long nanosToWait) {
    }

    /**
     * Cache of token bucket states keyed by "userId:endpointKey".
     */
    private final Map<String, TokenBucketState> bucketStates = new ConcurrentHashMap<>();

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
     *
     * @return true if rate limiting is enabled
     */
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /**
     * Attempts to consume a token for the given user and endpoint.
     * Uses a lightweight token bucket algorithm with O(1) complexity.
     *
     * @param userId      the user's unique identifier
     * @param endpointKey the endpoint key (e.g., "ai-summary")
     * @return consumption result with success status and remaining tokens
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

        // Atomic compute operation - O(1) with ConcurrentHashMap
        final ConsumptionResult[] result = new ConsumptionResult[1];

        bucketStates.compute(bucketKey, (key, state) -> {
            long nowNanos = System.nanoTime();

            if (state == null) {
                // First request - initialize with full capacity minus 1 consumed token
                result[0] = new ConsumptionResult(true, capacity - 1, 0);
                return new TokenBucketState(capacity - 1, nowNanos);
            }

            // Calculate elapsed time and tokens to add (greedy refill)
            long elapsedNanos = nowNanos - state.lastRefillNanos();
            double tokensToAdd = elapsedNanos * refillRatePerNano;
            double newTokens = Math.min(capacity, state.tokens() + tokensToAdd);

            if (newTokens >= 1.0) {
                // Consume 1 token
                double remaining = newTokens - 1.0;
                result[0] = new ConsumptionResult(true, (long) remaining, 0);
                return new TokenBucketState(remaining, nowNanos);
            } else {
                // Not enough tokens - calculate wait time
                double tokensNeeded = 1.0 - newTokens;
                long nanosToWait = (long) (tokensNeeded / refillRatePerNano);
                result[0] = new ConsumptionResult(false, 0, nanosToWait);
                // Don't update timestamp on rejection - preserve state
                return new TokenBucketState(newTokens, nowNanos);
            }
        });

        return result[0];
    }

    /**
     * Gets the remaining tokens for a user and endpoint without consuming.
     * Calculates current tokens based on elapsed time since last operation.
     *
     * @param userId      the user's unique identifier
     * @param endpointKey the endpoint key
     * @return remaining tokens, or max if no bucket exists
     */
    public long getRemainingTokens(String userId, String endpointKey) {
        if (!properties.isEnabled()) {
            return Long.MAX_VALUE;
        }

        String bucketKey = buildBucketKey(userId, endpointKey);
        TokenBucketState state = bucketStates.get(bucketKey);

        EndpointLimit limit = getLimitConfig(endpointKey);
        long capacity = limit.getMaxRequests();

        if (state == null) {
            return capacity;
        }

        // Calculate current tokens based on elapsed time
        long windowNanos = (long) limit.getWindowSeconds() * 1_000_000_000L;
        double refillRatePerNano = (double) capacity / windowNanos;
        long elapsedNanos = System.nanoTime() - state.lastRefillNanos();
        double tokensToAdd = elapsedNanos * refillRatePerNano;
        double currentTokens = Math.min(capacity, state.tokens() + tokensToAdd);

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
        bucketStates.clear();
        log.info("Rate limit bucket states cleared");
    }

    /**
     * Returns the current number of cached bucket states. Useful for monitoring.
     *
     * @return number of active bucket states
     */
    public int getBucketCount() {
        return bucketStates.size();
    }
}

