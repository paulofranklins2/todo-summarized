package org.duckdns.todosummarized.service;

import org.duckdns.todosummarized.dto.DailySummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiSummaryMessageBuilderTest {

    private AiSummaryMessageBuilder messageBuilder;
    private DailySummaryDTO sampleMetrics;

    @BeforeEach
    void setUp() {
        messageBuilder = new AiSummaryMessageBuilder();

        sampleMetrics = DailySummaryDTO.builder()
                .date(LocalDate.of(2026, 1, 9))
                .totalTodos(25)
                .completedCount(10)
                .inProgressCount(8)
                .notStartedCount(5)
                .cancelledCount(2)
                .overdueCount(3)
                .dueTodayCount(4)
                .upcomingCount(6)
                .completionRate(43.48)
                .byPriority(Map.of("HIGH", 5L, "MEDIUM", 12L, "LOW", 6L, "NONE", 2L))
                .byStatus(Map.of("COMPLETED", 10L, "IN_PROGRESS", 8L, "NOT_STARTED", 5L, "CANCELLED", 2L))
                .build();
    }

    @Nested
    @DisplayName("buildUserMessage")
    class BuildUserMessageTests {

        @Test
        @DisplayName("should include date in message")
        void shouldIncludeDateInMessage() {
            String message = messageBuilder.buildUserMessage(sampleMetrics);

            assertTrue(message.contains("2026-01-09"));
        }

        @Test
        @DisplayName("should include total todos count")
        void shouldIncludeTotalTodos() {
            String message = messageBuilder.buildUserMessage(sampleMetrics);

            assertTrue(message.contains("Total todos: 25"));
        }

        @Test
        @DisplayName("should include completed count")
        void shouldIncludeCompletedCount() {
            String message = messageBuilder.buildUserMessage(sampleMetrics);

            assertTrue(message.contains("Completed: 10"));
        }

        @Test
        @DisplayName("should include in progress count")
        void shouldIncludeInProgressCount() {
            String message = messageBuilder.buildUserMessage(sampleMetrics);

            assertTrue(message.contains("In Progress: 8"));
        }

        @Test
        @DisplayName("should include overdue count")
        void shouldIncludeOverdueCount() {
            String message = messageBuilder.buildUserMessage(sampleMetrics);

            assertTrue(message.contains("Overdue: 3"));
        }

        @Test
        @DisplayName("should include completion rate")
        void shouldIncludeCompletionRate() {
            String message = messageBuilder.buildUserMessage(sampleMetrics);

            assertTrue(message.contains("Completion Rate: 43.48%"));
        }

        @Test
        @DisplayName("should include priority breakdown")
        void shouldIncludePriorityBreakdown() {
            String message = messageBuilder.buildUserMessage(sampleMetrics);

            assertTrue(message.contains("By Priority:"));
            assertTrue(message.contains("HIGH"));
            assertTrue(message.contains("MEDIUM"));
            assertTrue(message.contains("LOW"));
        }

        @Test
        @DisplayName("should include status breakdown")
        void shouldIncludeStatusBreakdown() {
            String message = messageBuilder.buildUserMessage(sampleMetrics);

            assertTrue(message.contains("By Status:"));
            assertTrue(message.contains("COMPLETED"));
            assertTrue(message.contains("IN_PROGRESS"));
        }
    }

    @Nested
    @DisplayName("combinedPrompt")
    class CombinedPromptTests {

        @Test
        @DisplayName("should combine system prompt and user message")
        void shouldCombinePrompts() {
            String systemPrompt = "You are a helpful assistant.";
            String userMessage = "Here are my metrics.";

            String combined = messageBuilder.combinedPrompt(systemPrompt, userMessage);

            assertEquals("You are a helpful assistant.\n\nHere are my metrics.", combined);
        }

        @Test
        @DisplayName("should handle empty system prompt")
        void shouldHandleEmptySystemPrompt() {
            String combined = messageBuilder.combinedPrompt("", "User message");

            assertEquals("\n\nUser message", combined);
        }

        @Test
        @DisplayName("should handle empty user message")
        void shouldHandleEmptyUserMessage() {
            String combined = messageBuilder.combinedPrompt("System prompt", "");

            assertEquals("System prompt\n\n", combined);
        }
    }
}

