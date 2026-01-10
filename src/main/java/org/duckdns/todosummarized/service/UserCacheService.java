package org.duckdns.todosummarized.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for caching User entities
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final Cache<String, User> userCache;
    private final UserRepository userRepository;

    /**
     * Get a user by email with cache lookup.
     * Falls back to database if not in cache.
     */
    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }

        String normalizedEmail = email.toLowerCase();

        User cachedUser = userCache.getIfPresent(normalizedEmail);
        if (cachedUser != null) {
            log.info("USER CACHE HIT: {} (no database query)", normalizedEmail);
            return Optional.of(cachedUser);
        }

        log.info("USER CACHE MISS: {} (fetching from database)", normalizedEmail);
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        userOpt.ifPresent(user -> userCache.put(normalizedEmail, user));

        return userOpt;
    }

    /**
     * Invalidate a user from cache (e.g., after update or delete).
     */
    public void evict(String email) {
        if (email != null) {
            String normalizedEmail = email.toLowerCase();
            userCache.invalidate(normalizedEmail);
            log.debug("Evicted user from cache: {}", normalizedEmail);
        }
    }

    /**
     * Put a user into the cache.
     */
    public void put(User user) {
        if (user != null && user.getEmail() != null) {
            userCache.put(user.getEmail().toLowerCase(), user);
            log.debug("Cached user: {}", user.getEmail());
        }
    }

    /**
     * Clear all entries from the cache.
     */
    public void clearAll() {
        userCache.invalidateAll();
        log.info("Cleared all users from cache");
    }

    /**
     * Get cache statistics for monitoring.
     */
    public String getStats() {
        return userCache.stats().toString();
    }
}

