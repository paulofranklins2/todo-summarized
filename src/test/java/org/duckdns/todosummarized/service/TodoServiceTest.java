package org.duckdns.todosummarized.service;

import org.duckdns.todosummarized.domains.entity.Todo;
import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.Role;
import org.duckdns.todosummarized.domains.enums.TaskPriority;
import org.duckdns.todosummarized.domains.enums.TaskStatus;
import org.duckdns.todosummarized.dto.TodoRequestDTO;
import org.duckdns.todosummarized.exception.TodoNotFoundException;
import org.duckdns.todosummarized.repository.TodoQuery;
import org.duckdns.todosummarized.repository.TodoRepository;
import org.duckdns.todosummarized.repository.spec.TodoSpecs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    private Clock fixedClock;
    private TodoService todoService;
    private User user;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-01-08T12:00:00Z"), ZoneId.of("UTC"));
        todoService = new TodoService(todoRepository, fixedClock);
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("password")
                .role(Role.ROLE_USER)
                .build();
    }

    @Nested
    @DisplayName("createTodo tests")
    class CreateTodoTests {

        @Test
        @DisplayName("Should create a new todo successfully")
        void shouldCreateTodoSuccessfully() {
            // Given
            LocalDateTime dueDate = LocalDateTime.now().plusDays(7);
            TodoRequestDTO requestDTO = TodoRequestDTO.builder()
                    .title("Test Todo")
                    .description("Test Description")
                    .dueDate(dueDate)
                    .status(TaskStatus.IN_PROGRESS)
                    .priority(TaskPriority.HIGH)
                    .build();

            Todo savedTodo = new Todo();
            savedTodo.setId(UUID.randomUUID());
            savedTodo.setTitle("Test Todo");
            savedTodo.setDescription("Test Description");
            savedTodo.setDueDate(dueDate);
            savedTodo.setStatus(TaskStatus.IN_PROGRESS);
            savedTodo.setPriority(TaskPriority.HIGH);
            savedTodo.setUser(user);

            when(todoRepository.save(any(Todo.class))).thenReturn(savedTodo);

            // When
            Todo result = todoService.createTodo(requestDTO, user);

            // Then
            assertNotNull(result);
            assertEquals("Test Todo", result.getTitle());
            assertEquals("Test Description", result.getDescription());
            assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());
            assertEquals(TaskPriority.HIGH, result.getPriority());

            ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
            verify(todoRepository).save(captor.capture());
            assertEquals("Test Todo", captor.getValue().getTitle());
            assertEquals(user, captor.getValue().getUser());
        }

        @Test
        @DisplayName("Should create todo with default status and priority")
        void shouldCreateTodoWithDefaults() {
            // Given
            TodoRequestDTO requestDTO = TodoRequestDTO.builder()
                    .title("Todo with defaults")
                    .build();

            Todo savedTodo = new Todo();
            savedTodo.setId(UUID.randomUUID());
            savedTodo.setTitle("Todo with defaults");
            savedTodo.setStatus(TaskStatus.NOT_STARTED);
            savedTodo.setPriority(TaskPriority.LOW);
            savedTodo.setUser(user);

            when(todoRepository.save(any(Todo.class))).thenReturn(savedTodo);

            // When
            Todo result = todoService.createTodo(requestDTO, user);

            // Then
            assertNotNull(result);

            ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
            verify(todoRepository).save(captor.capture());
            assertEquals(TaskStatus.NOT_STARTED, captor.getValue().getStatus());
            assertEquals(TaskPriority.LOW, captor.getValue().getPriority());
            assertEquals(user, captor.getValue().getUser());
        }
    }

    @Nested
    @DisplayName("getTodoById tests")
    class GetTodoByIdTests {

        @Test
        @DisplayName("Should return todo when found")
        void shouldReturnTodoWhenFound() {
            // Given
            UUID id = UUID.randomUUID();
            Todo todo = new Todo();
            todo.setId(id);
            todo.setTitle("Found Todo");
            todo.setUser(user);

            when(todoRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(todo));

            // When
            Todo result = todoService.getTodoById(id, user);

            // Then
            assertNotNull(result);
            assertEquals(id, result.getId());
            assertEquals("Found Todo", result.getTitle());
            verify(todoRepository).findByIdAndUser(id, user);
        }

        @Test
        @DisplayName("Should throw TodoNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            // Given
            UUID id = UUID.randomUUID();
            when(todoRepository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

            // When & Then
            TodoNotFoundException exception = assertThrows(
                    TodoNotFoundException.class,
                    () -> todoService.getTodoById(id, user)
            );
            assertEquals("Todo not found with id: " + id, exception.getMessage());
            verify(todoRepository).findByIdAndUser(id, user);
        }
    }

    @Nested
    @DisplayName("updateTodo tests")
    class UpdateTodoTests {

        @Test
        @DisplayName("Should update all fields of an existing todo")
        void shouldUpdateAllFields() {
            // Given
            UUID id = UUID.randomUUID();
            LocalDateTime newDueDate = LocalDateTime.now().plusDays(14);

            Todo existingTodo = new Todo();
            existingTodo.setId(id);
            existingTodo.setTitle("Old Title");
            existingTodo.setDescription("Old Description");
            existingTodo.setStatus(TaskStatus.NOT_STARTED);
            existingTodo.setPriority(TaskPriority.LOW);
            existingTodo.setDueDate(LocalDateTime.now());
            existingTodo.setUser(user);

            TodoRequestDTO updateDTO = TodoRequestDTO.builder()
                    .title("New Title")
                    .description("New Description")
                    .status(TaskStatus.COMPLETED)
                    .priority(TaskPriority.CRITICAL)
                    .dueDate(newDueDate)
                    .build();

            when(todoRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(existingTodo));

            // When
            Todo result = todoService.updateTodo(id, updateDTO, user);

            // Then
            assertNotNull(result);
            assertEquals("New Title", result.getTitle());
            assertEquals("New Description", result.getDescription());
            assertEquals(TaskStatus.COMPLETED, result.getStatus());
            assertEquals(TaskPriority.CRITICAL, result.getPriority());
            assertEquals(newDueDate, result.getDueDate());
        }

        @Test
        @DisplayName("Should partially update todo - only title")
        void shouldPartiallyUpdateTodo() {
            // Given
            UUID id = UUID.randomUUID();
            LocalDateTime originalDueDate = LocalDateTime.now();

            Todo existingTodo = new Todo();
            existingTodo.setId(id);
            existingTodo.setTitle("Old Title");
            existingTodo.setDescription("Old Description");
            existingTodo.setStatus(TaskStatus.IN_PROGRESS);
            existingTodo.setPriority(TaskPriority.HIGH);
            existingTodo.setDueDate(originalDueDate);
            existingTodo.setUser(user);

            TodoRequestDTO updateDTO = TodoRequestDTO.builder()
                    .title("New Title")
                    .build();

            when(todoRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(existingTodo));

            // When
            Todo result = todoService.updateTodo(id, updateDTO, user);

            // Then
            assertNotNull(result);
            assertEquals("New Title", result.getTitle());
            assertEquals("Old Description", result.getDescription());
            assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());
            assertEquals(TaskPriority.HIGH, result.getPriority());
            assertEquals(originalDueDate, result.getDueDate());
        }

        @Test
        @DisplayName("Should throw TodoNotFoundException when updating non-existent todo")
        void shouldThrowWhenUpdatingNonExistent() {
            // Given
            UUID id = UUID.randomUUID();
            TodoRequestDTO updateDTO = TodoRequestDTO.builder()
                    .title("New Title")
                    .build();

            when(todoRepository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(
                    TodoNotFoundException.class,
                    () -> todoService.updateTodo(id, updateDTO, user)
            );
        }
    }

    @Nested
    @DisplayName("deleteTodo tests")
    class DeleteTodoTests {

        @Test
        @DisplayName("Should delete todo successfully")
        void shouldDeleteTodoSuccessfully() {
            // Given
            UUID id = UUID.randomUUID();
            when(todoRepository.deleteByIdAndUser(id, user)).thenReturn(1L);

            // When
            todoService.deleteTodo(id, user);

            // Then
            verify(todoRepository).deleteByIdAndUser(id, user);
        }

        @Test
        @DisplayName("Should throw TodoNotFoundException when deleting non-existent todo")
        void shouldThrowWhenDeletingNonExistent() {
            // Given
            UUID id = UUID.randomUUID();
            when(todoRepository.deleteByIdAndUser(id, user)).thenReturn(0L);

            // When & Then
            TodoNotFoundException exception = assertThrows(
                    TodoNotFoundException.class,
                    () -> todoService.deleteTodo(id, user)
            );
            assertEquals("Todo not found with id: " + id, exception.getMessage());
            verify(todoRepository).deleteByIdAndUser(id, user);
        }
    }

    @Nested
    @DisplayName("updateStatus tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update status successfully")
        void shouldUpdateStatusSuccessfully() {
            // Given
            UUID id = UUID.randomUUID();
            Todo existingTodo = new Todo();
            existingTodo.setId(id);
            existingTodo.setTitle("Test Todo");
            existingTodo.setStatus(TaskStatus.NOT_STARTED);
            existingTodo.setUser(user);

            when(todoRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(existingTodo));

            // When
            Todo result = todoService.updateStatus(id, TaskStatus.COMPLETED, user);

            // Then
            assertNotNull(result);
            assertEquals(TaskStatus.COMPLETED, result.getStatus());
            verify(todoRepository).findByIdAndUser(id, user);
        }

        @Test
        @DisplayName("Should update status from IN_PROGRESS to CANCELLED")
        void shouldUpdateStatusToCancelled() {
            // Given
            UUID id = UUID.randomUUID();
            Todo existingTodo = new Todo();
            existingTodo.setId(id);
            existingTodo.setStatus(TaskStatus.IN_PROGRESS);
            existingTodo.setUser(user);

            when(todoRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(existingTodo));

            // When
            Todo result = todoService.updateStatus(id, TaskStatus.CANCELLED, user);

            // Then
            assertEquals(TaskStatus.CANCELLED, result.getStatus());
        }

        @Test
        @DisplayName("Should throw TodoNotFoundException when updating status of non-existent todo")
        void shouldThrowWhenUpdatingStatusOfNonExistent() {
            // Given
            UUID id = UUID.randomUUID();
            when(todoRepository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(
                    TodoNotFoundException.class,
                    () -> todoService.updateStatus(id, TaskStatus.COMPLETED, user)
            );
        }
    }

    @Nested
    @DisplayName("search tests")
    class SearchTests {

        @Test
        @DisplayName("Should search with empty query returning all user todos")
        void shouldSearchWithEmptyQuery() {
            // Given
            TodoQuery query = new TodoQuery(null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            Todo todo1 = new Todo();
            todo1.setId(UUID.randomUUID());
            todo1.setTitle("Todo 1");
            todo1.setUser(user);

            Todo todo2 = new Todo();
            todo2.setId(UUID.randomUUID());
            todo2.setTitle("Todo 2");
            todo2.setUser(user);

            Page<Todo> expectedPage = new PageImpl<>(List.of(todo1, todo2), pageable, 2);
            when(todoRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

            // When
            Page<Todo> result = todoService.search(query, pageable, user);

            // Then
            assertNotNull(result);
            assertEquals(2, result.getTotalElements());
            assertEquals(2, result.getContent().size());
            verify(todoRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should search with status filter")
        void shouldSearchWithStatusFilter() {
            // Given
            TodoQuery query = new TodoQuery(TaskStatus.IN_PROGRESS, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            Todo todo = new Todo();
            todo.setId(UUID.randomUUID());
            todo.setTitle("In Progress Todo");
            todo.setStatus(TaskStatus.IN_PROGRESS);
            todo.setUser(user);

            Page<Todo> expectedPage = new PageImpl<>(List.of(todo), pageable, 1);
            when(todoRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

            // When
            Page<Todo> result = todoService.search(query, pageable, user);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(todoRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should search with priority filter")
        void shouldSearchWithPriorityFilter() {
            // Given
            TodoQuery query = new TodoQuery(null, TaskPriority.CRITICAL, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 5);

            Page<Todo> expectedPage = new PageImpl<>(List.of(), pageable, 0);
            when(todoRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

            // When
            Page<Todo> result = todoService.search(query, pageable, user);

            // Then
            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("Should search with date range filter")
        void shouldSearchWithDateRangeFilter() {
            // Given
            LocalDateTime dueFrom = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime dueTo = LocalDateTime.of(2026, 1, 31, 23, 59);
            TodoQuery query = new TodoQuery(null, null, dueFrom, dueTo, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            Todo todo = new Todo();
            todo.setId(UUID.randomUUID());
            todo.setTitle("January Todo");
            todo.setDueDate(LocalDateTime.of(2026, 1, 15, 12, 0));
            todo.setUser(user);

            Page<Todo> expectedPage = new PageImpl<>(List.of(todo), pageable, 1);
            when(todoRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

            // When
            Page<Todo> result = todoService.search(query, pageable, user);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Should search for overdue todos")
        void shouldSearchForOverdueTodos() {
            // Given
            TodoQuery query = new TodoQuery(null, null, null, null, true, null);
            Pageable pageable = PageRequest.of(0, 10);

            Todo overdueTodo = new Todo();
            overdueTodo.setId(UUID.randomUUID());
            overdueTodo.setTitle("Overdue Todo");
            overdueTodo.setDueDate(LocalDateTime.of(2026, 1, 1, 0, 0));
            overdueTodo.setUser(user);

            Page<Todo> expectedPage = new PageImpl<>(List.of(overdueTodo), pageable, 1);
            when(todoRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

            // When
            Page<Todo> result = todoService.search(query, pageable, user);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Should search for upcoming todos")
        void shouldSearchForUpcomingTodos() {
            // Given
            TodoQuery query = new TodoQuery(null, null, null, null, null, true);
            Pageable pageable = PageRequest.of(0, 10);

            Todo upcomingTodo = new Todo();
            upcomingTodo.setId(UUID.randomUUID());
            upcomingTodo.setTitle("Upcoming Todo");
            upcomingTodo.setDueDate(LocalDateTime.of(2026, 2, 1, 0, 0));
            upcomingTodo.setUser(user);

            Page<Todo> expectedPage = new PageImpl<>(List.of(upcomingTodo), pageable, 1);
            when(todoRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

            // When
            Page<Todo> result = todoService.search(query, pageable, user);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Should search with combined filters")
        void shouldSearchWithCombinedFilters() {
            // Given
            TodoQuery query = new TodoQuery(
                    TaskStatus.IN_PROGRESS,
                    TaskPriority.HIGH,
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 12, 31, 23, 59),
                    null,
                    null
            );
            Pageable pageable = PageRequest.of(0, 20);

            Page<Todo> expectedPage = new PageImpl<>(List.of(), pageable, 0);
            when(todoRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

            // When
            Page<Todo> result = todoService.search(query, pageable, user);

            // Then
            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("Should handle pagination correctly")
        void shouldHandlePaginationCorrectly() {
            // Given
            TodoQuery query = new TodoQuery(null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(2, 5); // Page 2, size 5

            Page<Todo> expectedPage = new PageImpl<>(List.of(), pageable, 25);
            when(todoRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

            // When
            Page<Todo> result = todoService.search(query, pageable, user);

            // Then
            assertNotNull(result);
            assertEquals(25, result.getTotalElements());
            assertEquals(5, result.getSize());
            assertEquals(2, result.getNumber());
        }
    }
}

