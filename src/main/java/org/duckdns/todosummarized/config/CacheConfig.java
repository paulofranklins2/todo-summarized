package org.duckdns.todosummarized.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.duckdns.todosummarized.domains.entity.Todo;
import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.dto.AiSummaryDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for in-memory caching using Caffeine.
 */
@Configuration
public class CacheConfig {
    private static final int MAX_CACHE_SIZE = 10_000;

    /**
     * User cache with lookup by email.
     */
    @Bean
    public Cache<String, User> userCache() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build();
    }

    /**
     * Todo search results cache
     * - Maximum 5,000 entries (search results per user/query combination)
     * - Entries expire 30 seconds after write (short TTL for freshness)
     * - Entries expire 1 minute after last access
     */
    @Bean
    public Cache<String, Page<Todo>> todoSearchCache() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /**
     * AI insight cache with one insight per user (keyed by user ID).
     * - When user generates a new insight, it replaces the existing one
     */
    @Bean
    public Cache<String, AiSummaryDTO> aiInsightCache() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .expireAfterAccess(2, TimeUnit.HOURS)
                .recordStats()
                .build();
    }

    /**
     * Rate limit token bucket cache.
     */
    @Bean
    public Cache<String, double[]> rateLimitCache() {
        return Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterAccess(1, TimeUnit.HOURS)
                .recordStats()
                .build();
    }
}

