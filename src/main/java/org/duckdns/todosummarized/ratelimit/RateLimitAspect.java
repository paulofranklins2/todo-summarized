package org.duckdns.todosummarized.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.exception.RateLimitExceededException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Aspect that intercepts methods annotated with {@link RateLimit} and applies rate limiting.
 * Uses the authenticated user's ID to create per-user rate limit buckets.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitService rateLimitService;

    /**
     * Intercepts methods annotated with @RateLimit and applies rate limiting.
     *
     * @param joinPoint  the join point representing the intercepted method
     * @param rateLimit  the rate limit annotation
     * @return the method result if allowed
     * @throws Throwable if rate limit exceeded or method throws
     */
    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        if (!rateLimitService.isEnabled()) {
            return joinPoint.proceed();
        }

        String userId = extractUserId();
        if (userId == null) {
            // No authenticated user, skip rate limiting (will be caught by security)
            return joinPoint.proceed();
        }

        String endpointKey = rateLimit.key();
        RateLimitService.ConsumptionResult result = rateLimitService.tryConsume(userId, endpointKey);

        if (result.consumed()) {
            log.debug("Rate limit check passed for user '{}' on endpoint '{}'. Remaining: {}",
                    userId, endpointKey, result.remainingTokens());
            return joinPoint.proceed();
        } else {
            long waitTimeSeconds = TimeUnit.NANOSECONDS.toSeconds(result.nanosToWait());
            log.warn("Rate limit exceeded for user '{}' on endpoint '{}'. Retry after: {} seconds",
                    userId, endpointKey, waitTimeSeconds);
            throw new RateLimitExceededException(waitTimeSeconds);
        }
    }

    /**
     * Extracts the user ID from the current security context.
     *
     * @return the user ID or null if not authenticated
     */
    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return user.getId().toString();
        } else if (principal instanceof String username) {
            return username;
        }

        return null;
    }
}

