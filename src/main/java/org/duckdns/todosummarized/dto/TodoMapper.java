package org.duckdns.todosummarized.dto;

import lombok.experimental.UtilityClass;
import org.duckdns.todosummarized.domains.entity.Todo;
import org.duckdns.todosummarized.domains.enums.TaskPriority;
import org.duckdns.todosummarized.domains.enums.TaskStatus;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Utility class for mapping between Todo entity and DTOs.
 */
@UtilityClass
public class TodoMapper {

    /**
     * Converts a Todo entity to a TodoResponseDTO.
     *
     * @param todo the Todo entity to convert
     * @return the corresponding TodoResponseDTO, or null if the input is null
     */
    public static TodoResponseDTO toResponseDTO(Todo todo) {
        if (todo == null) return null;

        return new TodoResponseDTO(
                todo.getId(),
                todo.getTitle(),
                todo.getDescription(),
                todo.getPriority(),
                todo.getStatus(),
                todo.getDueDate(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
    }

    /**
     * Converts a TodoRequestDTO to a new Todo entity.
     * Sets default values for status (NOT_STARTED) and priority (LOW) if not provided.
     *
     * @param dto the TodoRequestDTO to convert
     * @return a new Todo entity populated with the DTO values, or null if the input is null
     */
    public static Todo toNewEntity(TodoRequestDTO dto) {
        if (dto == null) return null;

        Todo todo = new Todo();
        todo.setTitle(dto.getTitle());
        todo.setDescription(dto.getDescription());
        todo.setDueDate(dto.getDueDate());
        todo.setStatus(defaultIfNull(dto.getStatus(), TaskStatus.NOT_STARTED));
        todo.setPriority(defaultIfNull(dto.getPriority(), TaskPriority.LOW));
        return todo;
    }

    /**
     * Patches an existing Todo entity with non-null values from a TodoRequestDTO.
     * Only updates fields that have non-null values in the DTO.
     *
     * @param dto  the TodoRequestDTO containing the updated values
     * @param todo the Todo entity to update (must not be null)
     * @throws NullPointerException if todo is null
     */
    public static void patchEntity(TodoRequestDTO dto, Todo todo) {
        if (dto == null) return;
        Objects.requireNonNull(todo, "todo must not be null");

        setIfNotNull(dto.getTitle(), todo::setTitle);
        setIfNotNull(dto.getDescription(), todo::setDescription);
        setIfNotNull(dto.getDueDate(), todo::setDueDate);
        setIfNotNull(dto.getStatus(), todo::setStatus);
        setIfNotNull(dto.getPriority(), todo::setPriority);
    }

    /**
     * Applies a value to a setter if the value is not null.
     *
     * @param value  the value to set
     * @param setter the setter consumer to apply the value
     * @param <T>    the type of the value
     */
    private static <T> void setIfNotNull(T value, Consumer<T> setter) {
        if (value != null) setter.accept(value);
    }

    /**
     * Returns the value if not null, otherwise returns the default value.
     *
     * @param value        the value to check
     * @param defaultValue the default value to return if value is null
     * @param <T>          the type of the value
     * @return the value if not null, otherwise the default value
     */
    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
