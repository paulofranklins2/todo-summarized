package org.duckdns.todosummarized.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.duckdns.todosummarized.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private RateLimitProperties properties;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setAiSummary(new RateLimitProperties.EndpointLimit(3, 60));
        properties.setDailySummary(new RateLimitProperties.EndpointLimit(5, 60));

        Cache<String, double[]> rateLimitCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build();

        rateLimitService = new RateLimitService(properties, rateLimitCache);
        rateLimitService.init();
    }

    @Nested
    @DisplayName("isEnabled")
    class IsEnabledTests {

        @Test
        @DisplayName("should return true when enabled")
        void shouldReturnTrueWhenEnabled() {
            assertTrue(rateLimitService.isEnabled());
        }

        @Test
        @DisplayName("should return false when disabled")
        void shouldReturnFalseWhenDisabled() {
            properties.setEnabled(false);
            assertFalse(rateLimitService.isEnabled());
        }
    }

    @Nested
    @DisplayName("tryConsume")
    class TryConsumeTests {

        @Test
        @DisplayName("should allow requests within limit")
        void shouldAllowRequestsWithinLimit() {
            String userId = "user-123";

            for (int i = 0; i < 3; i++) {
                RateLimitService.ConsumptionResult result = rateLimitService.tryConsume(userId, "ai-summary");
                assertTrue(result.consumed(), "Request " + (i + 1) + " should be allowed");
            }
        }

        @Test
        @DisplayName("should reject requests over limit")
        void shouldRejectRequestsOverLimit() {
            String userId = "user-456";

            // Consume all tokens
            for (int i = 0; i < 3; i++) {
                rateLimitService.tryConsume(userId, "ai-summary");
            }

            // Next request should be rejected
            RateLimitService.ConsumptionResult result = rateLimitService.tryConsume(userId, "ai-summary");
            assertFalse(result.consumed());
            assertTrue(result.nanosToWait() > 0);
        }

        @Test
        @DisplayName("should maintain separate limits per user")
        void shouldMaintainSeparateLimitsPerUser() {
            String user1 = "user-1";
            String user2 = "user-2";

            // User 1 exhausts their limit
            for (int i = 0; i < 3; i++) {
                rateLimitService.tryConsume(user1, "ai-summary");
            }

            // User 2 should still have tokens
            RateLimitService.ConsumptionResult result = rateLimitService.tryConsume(user2, "ai-summary");
            assertTrue(result.consumed());
        }

        @Test
        @DisplayName("should maintain separate limits per endpoint")
        void shouldMaintainSeparateLimitsPerEndpoint() {
            String userId = "user-789";

            // Exhaust AI summary limit
            for (int i = 0; i < 3; i++) {
                rateLimitService.tryConsume(userId, "ai-summary");
            }

            // Daily summary should still have tokens
            RateLimitService.ConsumptionResult result = rateLimitService.tryConsume(userId, "daily-summary");
            assertTrue(result.consumed());
        }

        @Test
        @DisplayName("should always allow when disabled")
        void shouldAlwaysAllowWhenDisabled() {
            properties.setEnabled(false);
            String userId = "user-disabled";

            // Even after many requests, should still allow
            for (int i = 0; i < 100; i++) {
                RateLimitService.ConsumptionResult result = rateLimitService.tryConsume(userId, "ai-summary");
                assertTrue(result.consumed());
            }
        }
    }

    @Nested
    @DisplayName("getRemainingTokens")
    class GetRemainingTokensTests {

        @Test
        @DisplayName("should return max tokens for new user")
        void shouldReturnMaxTokensForNewUser() {
            long remaining = rateLimitService.getRemainingTokens("new-user", "ai-summary");
            assertEquals(3, remaining);
        }

        @Test
        @DisplayName("should decrease after consumption")
        void shouldDecreaseAfterConsumption() {
            String userId = "consume-user";

            rateLimitService.tryConsume(userId, "ai-summary");

            long remaining = rateLimitService.getRemainingTokens(userId, "ai-summary");
            assertEquals(2, remaining);
        }

        @Test
        @DisplayName("should return max value when disabled")
        void shouldReturnMaxValueWhenDisabled() {
            properties.setEnabled(false);

            long remaining = rateLimitService.getRemainingTokens("any-user", "ai-summary");
            assertEquals(Long.MAX_VALUE, remaining);
        }
    }

    @Nested
    @DisplayName("clearBuckets")
    class ClearBucketsTests {

        @Test
        @DisplayName("should reset all buckets")
        void shouldResetAllBuckets() {
            String userId = "clear-user";

            // Consume some tokens
            rateLimitService.tryConsume(userId, "ai-summary");
            rateLimitService.tryConsume(userId, "ai-summary");

            assertEquals(1, rateLimitService.getRemainingTokens(userId, "ai-summary"));

            // Clear and check bucket count
            rateLimitService.clearBuckets();

            assertEquals(0, rateLimitService.getBucketCount());

            // New request should have full tokens
            assertEquals(3, rateLimitService.getRemainingTokens(userId, "ai-summary"));
        }
    }

    @Nested
    @DisplayName("getBucketCount")
    class GetBucketCountTests {

        @Test
        @DisplayName("should track active buckets")
        void shouldTrackActiveBuckets() {
            assertEquals(0, rateLimitService.getBucketCount());

            rateLimitService.tryConsume("user-1", "ai-summary");
            assertEquals(1, rateLimitService.getBucketCount());

            rateLimitService.tryConsume("user-1", "daily-summary");
            assertEquals(2, rateLimitService.getBucketCount());

            rateLimitService.tryConsume("user-2", "ai-summary");
            assertEquals(3, rateLimitService.getBucketCount());
        }
    }
}

