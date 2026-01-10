package org.duckdns.todosummarized.service;

import org.duckdns.todosummarized.dto.DailySummaryDTO;
import org.springframework.stereotype.Component;

/**
 * Utility class for building AI prompt messages from todo metrics.
 * Shared across all AI provider adapters to ensure consistent messaging.
 */
@Component
public class AiSummaryMessageBuilder {

    /**
     * Builds a user message containing todo metrics for AI summarization.
     *
     * @param metrics the daily summary metrics to format
     * @return formatted message string for the AI
     */
    public String buildUserMessage(DailySummaryDTO metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("Here are my todo metrics for today (").append(metrics.date()).append("):\n\n");
        sb.append("Total todos: ").append(metrics.totalTodos()).append("\n");
        sb.append("Completed: ").append(metrics.completedCount()).append("\n");
        sb.append("In Progress: ").append(metrics.inProgressCount()).append("\n");
        sb.append("Not Started: ").append(metrics.notStartedCount()).append("\n");
        sb.append("Cancelled: ").append(metrics.cancelledCount()).append("\n");
        sb.append("Overdue: ").append(metrics.overdueCount()).append("\n");
        sb.append("Due Today: ").append(metrics.dueTodayCount()).append("\n");
        sb.append("Upcoming (next 7 days): ").append(metrics.upcomingCount()).append("\n");
        sb.append("Completion Rate: ").append(metrics.completionRate()).append("%\n\n");

        sb.append("By Priority:\n");
        metrics.byPriority().forEach((priority, count) ->
                sb.append("  - ").append(priority).append(": ").append(count).append("\n"));

        sb.append("\nBy Status:\n");
        metrics.byStatus().forEach((status, count) ->
                sb.append("  - ").append(status).append(": ").append(count).append("\n"));

        return sb.toString();
    }

    /**
     * Combines system prompt and user message for providers that don't support
     * separate system/user messages (like Gemini's simpler API).
     *
     * @param systemPrompt the system prompt/instructions
     * @param userMessage  the user message with metrics
     * @return combined prompt string
     */
    public String combinedPrompt(String systemPrompt, String userMessage) {
        return systemPrompt + "\n\n" + userMessage;
    }
}

