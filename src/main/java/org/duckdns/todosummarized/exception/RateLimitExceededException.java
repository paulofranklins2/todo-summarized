package org.duckdns.todosummarized.exception;

/**
 * Exception thrown when a user exceeds the rate limit for an endpoint.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitExceededException(long retryAfterSeconds) {
        this("Rate limit exceeded. Please try again later.", retryAfterSeconds);
    }

    /**
     * Returns the number of seconds until the rate limit resets.
     *
     * @return seconds until retry is allowed
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

