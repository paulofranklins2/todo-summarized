package org.duckdns.todosummarized.dto;

import org.duckdns.todosummarized.domains.enums.TaskPriority;
import org.duckdns.todosummarized.domains.enums.TaskStatus;

import java.time.LocalDateTime;

public record TodoQuery(
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime dueFrom,
        LocalDateTime dueTo,
        Boolean overdue,
        Boolean upcoming
) {
}