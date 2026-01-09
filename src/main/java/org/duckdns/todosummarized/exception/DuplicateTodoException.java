package org.duckdns.todosummarized.exception;

/**
 * Exception thrown when attempting to create a duplicate Todo.
 */
public class DuplicateTodoException extends RuntimeException {

    public DuplicateTodoException(String message) {
        super(message);
    }

    public static DuplicateTodoException forTitle(String title) {
        return new DuplicateTodoException("Todo with title '" + title + "' already exists");
    }
}
