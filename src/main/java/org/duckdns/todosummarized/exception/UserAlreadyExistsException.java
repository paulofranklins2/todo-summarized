package org.duckdns.todosummarized.exception;

/**
 * Exception thrown when attempting to register with an email that already exists.
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String email) {
        super("User with email '" + email + "' already exists");
    }
}

