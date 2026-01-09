package org.duckdns.todosummarized.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.duckdns.todosummarized.domains.entity.Todo;
import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.TaskPriority;
import org.duckdns.todosummarized.domains.enums.TaskStatus;
import org.duckdns.todosummarized.dto.TodoMapper;
import org.duckdns.todosummarized.dto.TodoRequestDTO;
import org.duckdns.todosummarized.dto.TodoResponseDTO;
import org.duckdns.todosummarized.exception.ErrorResponse;
import org.duckdns.todosummarized.repository.TodoQuery;
import org.duckdns.todosummarized.service.TodoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST Controller for Todo CRUD operations.
 * All operations are scoped to the authenticated user.
 */
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
@Tag(name = "Todos", description = "Todo management API for CRUD operations and search")
public class TodoController {

    private final TodoService todoService;

    /**
     * Create a new todo for the authenticated user.
     */
    @Operation(summary = "Create a new todo", description = "Creates a new todo item for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Todo created successfully",
                    content = @Content(schema = @Schema(implementation = TodoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<TodoResponseDTO> createTodo(
            @Valid @RequestBody TodoRequestDTO request,
            @AuthenticationPrincipal User user) {
        Todo created = todoService.createTodo(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(TodoMapper.toResponseDTO(created));
    }

    /**
     * Get a todo by ID for the authenticated user.
     */
    @Operation(summary = "Get a todo by ID", description = "Retrieves a specific todo by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todo found",
                    content = @Content(schema = @Schema(implementation = TodoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Todo not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<TodoResponseDTO> getTodoById(
            @Parameter(description = "UUID of the todo to retrieve") @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        Todo todo = todoService.getTodoById(id, user);
        return ResponseEntity.ok(TodoMapper.toResponseDTO(todo));
    }

    /**
     * Update an existing todo for the authenticated user.
     */
    @Operation(summary = "Update a todo", description = "Updates an existing todo with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todo updated successfully",
                    content = @Content(schema = @Schema(implementation = TodoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Todo not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<TodoResponseDTO> updateTodo(
            @Parameter(description = "UUID of the todo to update") @PathVariable UUID id,
            @Valid @RequestBody TodoRequestDTO request,
            @AuthenticationPrincipal User user) {
        Todo updated = todoService.updateTodo(id, request, user);
        return ResponseEntity.ok(TodoMapper.toResponseDTO(updated));
    }

    /**
     * Delete a todo by ID for the authenticated user.
     */
    @Operation(summary = "Delete a todo", description = "Deletes a todo by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Todo deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Todo not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(
            @Parameter(description = "UUID of the todo to delete") @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        todoService.deleteTodo(id, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update only the status of a todo for the authenticated user.
     */
    @Operation(summary = "Update todo status", description = "Updates only the status of an existing todo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully",
                    content = @Content(schema = @Schema(implementation = TodoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Todo not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<TodoResponseDTO> updateStatus(
            @Parameter(description = "UUID of the todo to update") @PathVariable UUID id,
            @Parameter(description = "New status for the todo") @RequestParam TaskStatus status,
            @AuthenticationPrincipal User user) {
        Todo updated = todoService.updateStatus(id, status, user);
        return ResponseEntity.ok(TodoMapper.toResponseDTO(updated));
    }

    /**
     * Search todos with optional filters and pagination for the authenticated user.
     */
    @Operation(summary = "Search todos", description = "Search and filter todos with optional criteria and pagination")
    @ApiResponse(responseCode = "200", description = "Successful search")
    @GetMapping
    public ResponseEntity<Page<TodoResponseDTO>> searchTodos(
            @Parameter(description = "Filter by status") @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "Filter by priority") @RequestParam(required = false) TaskPriority priority,
            @Parameter(description = "Filter by due date from") @RequestParam(required = false) LocalDateTime dueFrom,
            @Parameter(description = "Filter by due date to") @RequestParam(required = false) LocalDateTime dueTo,
            @Parameter(description = "Filter overdue todos") @RequestParam(required = false) Boolean overdue,
            @Parameter(description = "Filter upcoming todos") @RequestParam(required = false) Boolean upcoming,
            @Parameter(hidden = true) Pageable pageable,
            @AuthenticationPrincipal User user) {

        TodoQuery query = new TodoQuery(status, priority, dueFrom, dueTo, overdue, upcoming);
        Page<Todo> todos = todoService.search(query, pageable, user);
        Page<TodoResponseDTO> response = todos.map(TodoMapper::toResponseDTO);

        return ResponseEntity.ok(response);
    }
}

