package org.duckdns.todosummarized.exception;

/**
 * Exception thrown when a Todo has invalid data.
 */
public class InvalidTodoException extends RuntimeException {

    public InvalidTodoException(String message) {
        super(message);
    }

    public InvalidTodoException(String message, Throwable cause) {
        super(message, cause);
    }
}

