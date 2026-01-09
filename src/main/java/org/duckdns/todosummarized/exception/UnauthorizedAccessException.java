package org.duckdns.todosummarized.exception;

/**
 * Exception thrown when a user tries to access a resource they don't own.
 */
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(String resourceType, String resourceId) {
        super("You do not have permission to access " + resourceType + " with id '" + resourceId + "'");
    }
}

