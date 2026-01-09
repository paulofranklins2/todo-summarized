package org.duckdns.todosummarized.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-01-08T12:00:00Z"), ZoneId.of("UTC"));
        handler = new GlobalExceptionHandler(fixedClock);
        when(request.getRequestURI()).thenReturn("/api/todos");
    }

    @Nested
    @DisplayName("handleTodoNotFound tests")
    class HandleTodoNotFoundTests {

        @Test
        @DisplayName("Should return 404 NOT_FOUND with correct error response")
        void shouldReturnNotFoundStatus() {
            // Given
            UUID id = UUID.randomUUID();
            TodoNotFoundException ex = new TodoNotFoundException(id);

            // When
            ResponseEntity<ErrorResponse> response = handler.handleTodoNotFound(ex, request);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(404, body.getStatus());
            assertEquals("Not Found", body.getError());
            assertEquals("Todo not found with id: " + id, body.getMessage());
            assertEquals("/api/todos", body.getPath());
            assertEquals(LocalDateTime.now(fixedClock), body.getTimestamp());
            assertNull(body.getFieldErrors());
        }
    }

    @Nested
    @DisplayName("handleInvalidTodo tests")
    class HandleInvalidTodoTests {

        @Test
        @DisplayName("Should return 400 BAD_REQUEST with correct error response")
        void shouldReturnBadRequestStatus() {
            // Given
            InvalidTodoException ex = new InvalidTodoException("Title cannot be empty");

            // When
            ResponseEntity<ErrorResponse> response = handler.handleInvalidTodo(ex, request);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(400, body.getStatus());
            assertEquals("Bad Request", body.getError());
            assertEquals("Title cannot be empty", body.getMessage());
            assertEquals("/api/todos", body.getPath());
            assertNull(body.getFieldErrors());
        }
    }

    @Nested
    @DisplayName("handleDuplicateTodo tests")
    class HandleDuplicateTodoTests {

        @Test
        @DisplayName("Should return 409 CONFLICT with correct error response")
        void shouldReturnConflictStatus() {
            // Given
            DuplicateTodoException ex = DuplicateTodoException.forTitle("Buy groceries");

            // When
            ResponseEntity<ErrorResponse> response = handler.handleDuplicateTodo(ex, request);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(409, body.getStatus());
            assertEquals("Conflict", body.getError());
            assertEquals("Todo with title 'Buy groceries' already exists", body.getMessage());
            assertEquals("/api/todos", body.getPath());
            assertNull(body.getFieldErrors());
        }

        @Test
        @DisplayName("Should handle DuplicateTodoException with custom message")
        void shouldHandleCustomMessage() {
            // Given
            DuplicateTodoException ex = new DuplicateTodoException("Custom duplicate message");

            // When
            ResponseEntity<ErrorResponse> response = handler.handleDuplicateTodo(ex, request);

            // Then
            assertNotNull(response);
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals("Custom duplicate message", body.getMessage());
        }
    }

    @Nested
    @DisplayName("handleValidation tests")
    class HandleValidationTests {

        @Mock
        private BindingResult bindingResult;

        @Test
        @DisplayName("Should return 400 BAD_REQUEST with field errors")
        void shouldReturnBadRequestWithFieldErrors() {
            // Given
            FieldError fieldError1 = new FieldError("todoRequest", "title", null, false, null, null, "Title is required");
            FieldError fieldError2 = new FieldError("todoRequest", "description", "x".repeat(1001), false, null, null, "Description must not exceed 1000 characters");

            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            // When
            ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(400, body.getStatus());
            assertEquals("Bad Request", body.getError());
            assertEquals("Validation failed", body.getMessage());
            assertEquals("/api/todos", body.getPath());

            List<ErrorResponse.FieldError> fieldErrors = body.getFieldErrors();
            assertNotNull(fieldErrors);
            assertEquals(2, fieldErrors.size());

            assertEquals("title", fieldErrors.get(0).getField());
            assertEquals("Title is required", fieldErrors.get(0).getMessage());
            assertNull(fieldErrors.get(0).getRejectedValue());

            assertEquals("description", fieldErrors.get(1).getField());
            assertEquals("Description must not exceed 1000 characters", fieldErrors.get(1).getMessage());
            assertEquals("x".repeat(1001), fieldErrors.get(1).getRejectedValue());
        }

        @Test
        @DisplayName("Should handle empty field errors list")
        void shouldHandleEmptyFieldErrors() {
            // Given
            when(bindingResult.getFieldErrors()).thenReturn(List.of());
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            // When
            ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

            // Then
            assertNotNull(response);
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals("Validation failed", body.getMessage());
            assertNotNull(body.getFieldErrors());
            assertTrue(body.getFieldErrors().isEmpty());
        }
    }

    @Nested
    @DisplayName("handleTypeMismatch tests")
    class HandleTypeMismatchTests {

        @Test
        @DisplayName("Should return 400 BAD_REQUEST with type mismatch details")
        void shouldReturnBadRequestWithTypeMismatchDetails() {
            // Given
            MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
            when(ex.getName()).thenReturn("id");
            when(ex.getValue()).thenReturn("invalid-uuid");
            when(ex.getRequiredType()).thenReturn((Class) UUID.class);

            // When
            ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(400, body.getStatus());
            assertEquals("Bad Request", body.getError());
            assertEquals("Invalid value 'invalid-uuid' for parameter 'id'. Expected: UUID", body.getMessage());
            assertEquals("/api/todos", body.getPath());
            assertNull(body.getFieldErrors());
        }

        @Test
        @DisplayName("Should handle null required type")
        void shouldHandleNullRequiredType() {
            // Given
            MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
            when(ex.getName()).thenReturn("status");
            when(ex.getValue()).thenReturn("INVALID_STATUS");
            when(ex.getRequiredType()).thenReturn(null);

            // When
            ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request);

            // Then
            assertNotNull(response);
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals("Invalid value 'INVALID_STATUS' for parameter 'status'. Expected: unknown", body.getMessage());
        }
    }

    @Nested
    @DisplayName("handleBadJson tests")
    class HandleBadJsonTests {

        @Test
        @DisplayName("Should return 400 BAD_REQUEST for malformed JSON")
        void shouldReturnBadRequestForMalformedJson() {
            // Given
            HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
            when(ex.getMessage()).thenReturn("JSON parse error: Unexpected character");

            // When
            ResponseEntity<ErrorResponse> response = handler.handleBadJson(ex, request);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(400, body.getStatus());
            assertEquals("Bad Request", body.getError());
            assertEquals("Malformed JSON request body", body.getMessage());
            assertEquals("/api/todos", body.getPath());
            assertNull(body.getFieldErrors());
        }
    }

    @Nested
    @DisplayName("handleGeneric tests")
    class HandleGenericTests {

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR for unexpected exceptions")
        void shouldReturnInternalServerError() {
            // Given
            Exception ex = new RuntimeException("Unexpected database error");

            // When
            ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex, request);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(500, body.getStatus());
            assertEquals("Internal Server Error", body.getError());
            assertEquals("An unexpected error occurred. Please try again later.", body.getMessage());
            assertEquals("/api/todos", body.getPath());
            assertNull(body.getFieldErrors());
        }

        @Test
        @DisplayName("Should handle NullPointerException")
        void shouldHandleNullPointerException() {
            // Given
            Exception ex = new NullPointerException("Something was null");

            // When
            ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex, request);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals("An unexpected error occurred. Please try again later.", body.getMessage());
        }
    }

    @Nested
    @DisplayName("Response structure tests")
    class ResponseStructureTests {

        @Test
        @DisplayName("Should include timestamp from clock")
        void shouldIncludeTimestampFromClock() {
            // Given
            TodoNotFoundException ex = new TodoNotFoundException(UUID.randomUUID());

            // When
            ResponseEntity<ErrorResponse> response = handler.handleTodoNotFound(ex, request);

            // Then
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(LocalDateTime.of(2026, 1, 8, 12, 0, 0), body.getTimestamp());
        }

        @Test
        @DisplayName("Should include request path in response")
        void shouldIncludeRequestPath() {
            // Given
            when(request.getRequestURI()).thenReturn("/api/todos/123");
            InvalidTodoException ex = new InvalidTodoException("Invalid");

            // When
            ResponseEntity<ErrorResponse> response = handler.handleInvalidTodo(ex, request);

            // Then
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals("/api/todos/123", body.getPath());
        }
    }
}

