package org.duckdns.todosummarized.controller;

import org.duckdns.todosummarized.domains.entity.Todo;
import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.Role;
import org.duckdns.todosummarized.domains.enums.TaskPriority;
import org.duckdns.todosummarized.domains.enums.TaskStatus;
import org.duckdns.todosummarized.dto.TodoRequestDTO;
import org.duckdns.todosummarized.dto.TodoResponseDTO;
import org.duckdns.todosummarized.exception.TodoNotFoundException;
import org.duckdns.todosummarized.repository.TodoQuery;
import org.duckdns.todosummarized.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoControllerTest {

    @Mock
    private TodoService todoService;

    @InjectMocks
    private TodoController todoController;

    private UUID id;
    private Todo todo;
    private TodoRequestDTO request;
    private User user;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();

        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("password")
                .role(Role.ROLE_USER)
                .build();

        todo = new Todo();
        todo.setId(id);
        todo.setTitle("Test Todo");
        todo.setDescription("Test Description");
        todo.setPriority(TaskPriority.MEDIUM);
        todo.setStatus(TaskStatus.NOT_STARTED);
        todo.setUser(user);

        request = TodoRequestDTO.builder()
                .title("Test Todo")
                .description("Test Description")
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.NOT_STARTED)
                .build();
    }

    /**
     * createTodo returns 201 and mapped response
     */
    @Test
    void createTodo_returnsCreated() {
        when(todoService.createTodo(any(), any(User.class))).thenReturn(todo);

        ResponseEntity<TodoResponseDTO> response =
                todoController.createTodo(request, user);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(id, response.getBody().id());

        verify(todoService).createTodo(request, user);
    }

    /**
     * getTodoById returns todo when found
     */
    @Test
    void getTodoById_returnsTodo() {
        when(todoService.getTodoById(id, user)).thenReturn(todo);

        ResponseEntity<TodoResponseDTO> response =
                todoController.getTodoById(id, user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().id());
    }

    /**
     * getTodoById throws when todo does not exist
     */
    @Test
    void getTodoById_throwsWhenNotFound() {
        when(todoService.getTodoById(id, user))
                .thenThrow(new TodoNotFoundException(id));

        assertThrows(
                TodoNotFoundException.class,
                () -> todoController.getTodoById(id, user)
        );
    }

    /**
     * updateTodo returns updated todo
     */
    @Test
    void updateTodo_returnsUpdatedTodo() {
        when(todoService.updateTodo(eq(id), any(), eq(user)))
                .thenReturn(todo);

        ResponseEntity<TodoResponseDTO> response =
                todoController.updateTodo(id, request, user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().id());
    }

    /**
     * deleteTodo returns 204
     */
    @Test
    void deleteTodo_returnsNoContent() {
        doNothing().when(todoService).deleteTodo(id, user);

        ResponseEntity<Void> response =
                todoController.deleteTodo(id, user);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }

    /**
     * updateStatus updates todo status
     */
    @Test
    void updateStatus_updatesStatus() {
        todo.setStatus(TaskStatus.COMPLETED);
        when(todoService.updateStatus(id, TaskStatus.COMPLETED, user))
                .thenReturn(todo);

        ResponseEntity<TodoResponseDTO> response =
                todoController.updateStatus(id, TaskStatus.COMPLETED, user);

        assertEquals(TaskStatus.COMPLETED, response.getBody().status());
    }

    /**
     * searchTodos passes filters to service
     */
    @Test
    void searchTodos_passesFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        when(todoService.search(any(TodoQuery.class), eq(pageable), eq(user)))
                .thenReturn(Page.empty(pageable));

        todoController.searchTodos(
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH,
                null,
                null,
                true,
                false,
                pageable,
                user
        );

        ArgumentCaptor<TodoQuery> captor =
                ArgumentCaptor.forClass(TodoQuery.class);

        verify(todoService).search(captor.capture(), eq(pageable), eq(user));

        TodoQuery query = captor.getValue();
        assertEquals(TaskStatus.IN_PROGRESS, query.status());
        assertEquals(TaskPriority.HIGH, query.priority());
        assertTrue(query.overdue());
        assertFalse(query.upcoming());
    }
}
