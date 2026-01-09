package org.duckdns.todosummarized.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply rate limiting to a controller method.
 * Rate limits are applied per-user based on the authenticated principal.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Unique key for this rate limit bucket.
     * Combined with user ID to create per-user rate limits.
     *
     * @return the rate limit key
     */
    String key();
}

