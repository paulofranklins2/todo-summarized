package org.duckdns.todosummarized.dto;

import org.duckdns.todosummarized.domains.entity.Todo;
import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.Role;
import org.duckdns.todosummarized.domains.enums.TaskPriority;
import org.duckdns.todosummarized.domains.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TodoMapperTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("password")
                .role(Role.ROLE_USER)
                .build();
    }

    @Nested
    @DisplayName("toResponseDTO tests")
    class ToResponseDTOTests {

        @Test
        @DisplayName("Should return null when todo is null")
        void shouldReturnNullWhenTodoIsNull() {
            TodoResponseDTO result = TodoMapper.toResponseDTO(null);
            assertNull(result);
        }

        @Test
        @DisplayName("Should convert Todo entity to TodoResponseDTO correctly")
        void shouldConvertTodoToResponseDTO() {
            // Given
            UUID id = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime dueDate = now.plusDays(7);

            Todo todo = new Todo();
            todo.setId(id);
            todo.setTitle("Test Title");
            todo.setDescription("Test Description");
            todo.setPriority(TaskPriority.HIGH);
            todo.setStatus(TaskStatus.IN_PROGRESS);
            todo.setDueDate(dueDate);
            todo.setCreatedAt(now);
            todo.setUpdatedAt(now);
            todo.setUser(user);

            // When
            TodoResponseDTO result = TodoMapper.toResponseDTO(todo);

            // Then
            assertNotNull(result);
            assertEquals(id, result.id());
            assertEquals("Test Title", result.title());
            assertEquals("Test Description", result.description());
            assertEquals(TaskPriority.HIGH, result.priority());
            assertEquals(TaskStatus.IN_PROGRESS, result.status());
            assertEquals(dueDate, result.dueDate());
            assertEquals(now, result.createdAt());
            assertEquals(now, result.updatedAt());
        }

        @Test
        @DisplayName("Should handle Todo with null optional fields")
        void shouldHandleTodoWithNullOptionalFields() {
            // Given
            Todo todo = new Todo();
            todo.setTitle("Title Only");

            // When
            TodoResponseDTO result = TodoMapper.toResponseDTO(todo);

            // Then
            assertNotNull(result);
            assertEquals("Title Only", result.title());
            assertNull(result.id());
            assertNull(result.description());
            assertNull(result.priority());
            assertNull(result.status());
            assertNull(result.dueDate());
            assertNull(result.createdAt());
            assertNull(result.updatedAt());
        }
    }

    @Nested
    @DisplayName("toNewEntity tests")
    class ToNewEntityTests {

        @Test
        @DisplayName("Should return null when dto is null")
        void shouldReturnNullWhenDtoIsNull() {
            Todo result = TodoMapper.toNewEntity(null, user);
            assertNull(result);
        }

        @Test
        @DisplayName("Should convert TodoRequestDTO to Todo entity with all fields")
        void shouldConvertDtoToEntityWithAllFields() {
            // Given
            LocalDateTime dueDate = LocalDateTime.now().plusDays(7);
            TodoRequestDTO dto = TodoRequestDTO.builder()
                    .title("Test Title")
                    .description("Test Description")
                    .dueDate(dueDate)
                    .status(TaskStatus.IN_PROGRESS)
                    .priority(TaskPriority.HIGH)
                    .build();

            // When
            Todo result = TodoMapper.toNewEntity(dto, user);

            // Then
            assertNotNull(result);
            assertEquals("Test Title", result.getTitle());
            assertEquals("Test Description", result.getDescription());
            assertEquals(dueDate, result.getDueDate());
            assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());
            assertEquals(TaskPriority.HIGH, result.getPriority());
            assertEquals(user, result.getUser());
        }

        @Test
        @DisplayName("Should set default status to NOT_STARTED when null")
        void shouldSetDefaultStatusWhenNull() {
            // Given
            TodoRequestDTO dto = TodoRequestDTO.builder()
                    .title("Test Title")
                    .priority(TaskPriority.MEDIUM)
                    .build();

            // When
            Todo result = TodoMapper.toNewEntity(dto, user);

            // Then
            assertNotNull(result);
            assertEquals(TaskStatus.NOT_STARTED, result.getStatus());
            assertEquals(user, result.getUser());
        }

        @Test
        @DisplayName("Should set default priority to LOW when null")
        void shouldSetDefaultPriorityWhenNull() {
            // Given
            TodoRequestDTO dto = TodoRequestDTO.builder()
                    .title("Test Title")
                    .status(TaskStatus.COMPLETED)
                    .build();

            // When
            Todo result = TodoMapper.toNewEntity(dto, user);

            // Then
            assertNotNull(result);
            assertEquals(TaskPriority.LOW, result.getPriority());
            assertEquals(user, result.getUser());
        }

        @Test
        @DisplayName("Should set both defaults when status and priority are null")
        void shouldSetBothDefaultsWhenNull() {
            // Given
            TodoRequestDTO dto = TodoRequestDTO.builder()
                    .title("Test Title")
                    .description("Description")
                    .build();

            // When
            Todo result = TodoMapper.toNewEntity(dto, user);

            // Then
            assertNotNull(result);
            assertEquals(TaskStatus.NOT_STARTED, result.getStatus());
            assertEquals(TaskPriority.LOW, result.getPriority());
            assertEquals(user, result.getUser());
        }
    }

    @Nested
    @DisplayName("patchEntity tests")
    class PatchEntityTests {

        @Test
        @DisplayName("Should do nothing when dto is null")
        void shouldDoNothingWhenDtoIsNull() {
            // Given
            Todo todo = new Todo();
            todo.setTitle("Original Title");
            todo.setDescription("Original Description");

            // When
            TodoMapper.patchEntity(null, todo);

            // Then
            assertEquals("Original Title", todo.getTitle());
            assertEquals("Original Description", todo.getDescription());
        }

        @Test
        @DisplayName("Should throw NullPointerException when todo is null")
        void shouldThrowWhenTodoIsNull() {
            // Given
            TodoRequestDTO dto = TodoRequestDTO.builder()
                    .title("New Title")
                    .build();

            // When & Then
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> TodoMapper.patchEntity(dto, null)
            );
            assertEquals("todo must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should update all fields when all are provided")
        void shouldUpdateAllFieldsWhenProvided() {
            // Given
            LocalDateTime newDueDate = LocalDateTime.now().plusDays(14);
            Todo todo = new Todo();
            todo.setTitle("Old Title");
            todo.setDescription("Old Description");
            todo.setStatus(TaskStatus.NOT_STARTED);
            todo.setPriority(TaskPriority.LOW);
            todo.setDueDate(LocalDateTime.now());

            TodoRequestDTO dto = TodoRequestDTO.builder()
                    .title("New Title")
                    .description("New Description")
                    .status(TaskStatus.COMPLETED)
                    .priority(TaskPriority.CRITICAL)
                    .dueDate(newDueDate)
                    .build();

            // When
            TodoMapper.patchEntity(dto, todo);

            // Then
            assertEquals("New Title", todo.getTitle());
            assertEquals("New Description", todo.getDescription());
            assertEquals(TaskStatus.COMPLETED, todo.getStatus());
            assertEquals(TaskPriority.CRITICAL, todo.getPriority());
            assertEquals(newDueDate, todo.getDueDate());
        }

        @Test
        @DisplayName("Should only update non-null fields - title only")
        void shouldOnlyUpdateTitleWhenOthersNull() {
            // Given
            LocalDateTime originalDueDate = LocalDateTime.now();
            Todo todo = new Todo();
            todo.setTitle("Old Title");
            todo.setDescription("Old Description");
            todo.setStatus(TaskStatus.IN_PROGRESS);
            todo.setPriority(TaskPriority.HIGH);
            todo.setDueDate(originalDueDate);

            TodoRequestDTO dto = TodoRequestDTO.builder()
                    .title("New Title")
                    .build();

            // When
            TodoMapper.patchEntity(dto, todo);

            // Then
            assertEquals("New Title", todo.getTitle());
            assertEquals("Old Description", todo.getDescription());
            assertEquals(TaskStatus.IN_PROGRESS, todo.getStatus());
            assertEquals(TaskPriority.HIGH, todo.getPriority());
            assertEquals(originalDueDate, todo.getDueDate());
        }

        @Test
        @DisplayName("Should only update non-null fields - description only")
        void shouldOnlyUpdateDescriptionWhenOthersNull() {
            // Given
            Todo todo = new Todo();
            todo.setTitle("Original Title");
            todo.setDescription("Original Description");

            TodoRequestDTO dto = TodoRequestDTO.builder()
                    .description("New Description")
                    .build();

            // When
            TodoMapper.patchEntity(dto, todo);

            // Then
            assertEquals("Original Title", todo.getTitle());
            assertEquals("New Description", todo.getDescription());
        }

        @Test
        @DisplayName("Should only update non-null fields - status only")
        void shouldOnlyUpdateStatusWhenOthersNull() {
            // Given
            Todo todo = new Todo();
            todo.setTitle("Original Title");
            todo.setStatus(TaskStatus.NOT_STARTED);

            TodoRequestDTO dto = TodoRequestDTO.builder()
                    .status(TaskStatus.CANCELLED)
                    .build();

            // When
            TodoMapper.patchEntity(dto, todo);

            // Then
            assertEquals("Original Title", todo.getTitle());
            assertEquals(TaskStatus.CANCELLED, todo.getStatus());
        }

        @Test
        @DisplayName("Should only update non-null fields - priority only")
        void shouldOnlyUpdatePriorityWhenOthersNull() {
            // Given
            Todo todo = new Todo();
            todo.setTitle("Original Title");
            todo.setPriority(TaskPriority.LOW);

            TodoRequestDTO dto = TodoRequestDTO.builder()
                    .priority(TaskPriority.MEDIUM)
                    .build();

            // When
            TodoMapper.patchEntity(dto, todo);

            // Then
            assertEquals("Original Title", todo.getTitle());
            assertEquals(TaskPriority.MEDIUM, todo.getPriority());
        }

        @Test
        @DisplayName("Should only update non-null fields - dueDate only")
        void shouldOnlyUpdateDueDateWhenOthersNull() {
            // Given
            LocalDateTime originalDueDate = LocalDateTime.now();
            LocalDateTime newDueDate = LocalDateTime.now().plusDays(30);
            Todo todo = new Todo();
            todo.setTitle("Original Title");
            todo.setDueDate(originalDueDate);

            TodoRequestDTO dto = TodoRequestDTO.builder()
                    .dueDate(newDueDate)
                    .build();

            // When
            TodoMapper.patchEntity(dto, todo);

            // Then
            assertEquals("Original Title", todo.getTitle());
            assertEquals(newDueDate, todo.getDueDate());
        }

        @Test
        @DisplayName("Should not update any field when all dto fields are null")
        void shouldNotUpdateWhenAllDtoFieldsAreNull() {
            // Given
            LocalDateTime dueDate = LocalDateTime.now();
            Todo todo = new Todo();
            todo.setTitle("Original Title");
            todo.setDescription("Original Description");
            todo.setStatus(TaskStatus.IN_PROGRESS);
            todo.setPriority(TaskPriority.HIGH);
            todo.setDueDate(dueDate);

            TodoRequestDTO dto = TodoRequestDTO.builder().build();

            // When
            TodoMapper.patchEntity(dto, todo);

            // Then
            assertEquals("Original Title", todo.getTitle());
            assertEquals("Original Description", todo.getDescription());
            assertEquals(TaskStatus.IN_PROGRESS, todo.getStatus());
            assertEquals(TaskPriority.HIGH, todo.getPriority());
            assertEquals(dueDate, todo.getDueDate());
        }
    }
}

