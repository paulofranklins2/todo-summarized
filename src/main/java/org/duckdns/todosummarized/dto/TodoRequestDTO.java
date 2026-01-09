package org.duckdns.todosummarized.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.duckdns.todosummarized.domains.enums.TaskPriority;
import org.duckdns.todosummarized.domains.enums.TaskStatus;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoRequestDTO {
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    private LocalDateTime dueDate;
    private TaskStatus status;
    private TaskPriority priority;

}
