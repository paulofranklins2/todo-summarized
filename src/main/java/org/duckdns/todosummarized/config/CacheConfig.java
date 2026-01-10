package org.duckdns.todosummarized.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.duckdns.todosummarized.domains.entity.Todo;
import org.duckdns.todosummarized.domains.entity.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for in-memory caching using Caffeine.
 * Provides O(1) lookup time for cached entities.
 */
@Configuration
public class CacheConfig {

    /**
     * User cache with O(1) lookup by email.
     * - Maximum 10,000 entries
     * - Entries expire 15 minutes after last access
     * - Entries expire 1 hour after creation (to refresh from DB periodically)
     */
    @Bean
    public Cache<String, User> userCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build();
    }

    /**
     * Todo search results cache with O(1) lookup.
     * - Maximum 5,000 entries (search results per user/query combination)
     * - Entries expire 30 seconds after write (short TTL for freshness)
     * - Entries expire 1 minute after last access
     */
    @Bean
    public Cache<String, Page<Todo>> todoSearchCache() {
        return Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}

