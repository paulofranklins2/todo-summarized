package org.duckdns.todosummarized.service;

import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.Role;
import org.duckdns.todosummarized.domains.enums.TaskPriority;
import org.duckdns.todosummarized.domains.enums.TaskStatus;
import org.duckdns.todosummarized.dto.DailySummaryDTO;
import org.duckdns.todosummarized.repository.TodoRepository;
import org.duckdns.todosummarized.repository.projection.PriorityCountProjection;
import org.duckdns.todosummarized.repository.projection.StatusCountProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private SummaryService summaryService;

    private User user;

    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 1, 9);
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    @BeforeEach
    void setUp() {
        Instant fixedInstant = FIXED_DATE.atStartOfDay(ZONE_ID).toInstant();
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(ZONE_ID);

        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("password")
                .role(Role.ROLE_USER)
                .build();
    }

    @Nested
    @DisplayName("getDailySummary")
    class GetDailySummaryTests {

        @Test
        @DisplayName("should return correct date")
        void shouldReturnCorrectDate() {
            mockAllCounts(0, 0, 0, 0, 0, 0, 0, 0);
            mockPriorityCounts(0, 0, 0, 0);

            DailySummaryDTO result = summaryService.getDailySummary(user);

            assertEquals(FIXED_DATE, result.date());
        }

        @Test
        @DisplayName("should return total count")
        void shouldReturnTotalCount() {
            when(todoRepository.countByUser(user)).thenReturn(25L);
            mockStatusCounts(10, 8, 5, 2);
            mockTimeCounts(3, 4, 6);
            mockPriorityCounts(5, 10, 8, 2);

            DailySummaryDTO result = summaryService.getDailySummary(user);

            assertEquals(25L, result.totalTodos());
        }

        @Test
        @DisplayName("should return status counts")
        void shouldReturnStatusCounts() {
            mockAllCounts(20, 8, 6, 4, 2, 3, 2, 5);
            mockPriorityCounts(5, 8, 5, 2);

            DailySummaryDTO result = summaryService.getDailySummary(user);

            assertEquals(8L, result.completedCount());
            assertEquals(6L, result.inProgressCount());
            assertEquals(4L, result.notStartedCount());
            assertEquals(2L, result.cancelledCount());
        }

        @Test
        @DisplayName("should return time-based counts")
        void shouldReturnTimeBasedCounts() {
            mockAllCounts(20, 10, 5, 3, 2, 4, 3, 6);
            mockPriorityCounts(5, 8, 5, 2);

            DailySummaryDTO result = summaryService.getDailySummary(user);

            assertEquals(4L, result.overdueCount());
            assertEquals(3L, result.dueTodayCount());
            assertEquals(6L, result.upcomingCount());
        }

        @Test
        @DisplayName("should calculate completion rate correctly")
        void shouldCalculateCompletionRate() {
            // 10 completed out of 18 active (20 total - 2 cancelled) = 55.56%
            mockAllCounts(20, 10, 5, 3, 2, 3, 2, 5);
            mockPriorityCounts(5, 8, 5, 2);

            DailySummaryDTO result = summaryService.getDailySummary(user);

            assertEquals(55.56, result.completionRate(), 0.01);
        }

        @Test
        @DisplayName("should return 0 completion rate when no active todos")
        void shouldReturnZeroCompletionRateWhenNoActiveTodos() {
            // All 5 todos are cancelled
            mockAllCounts(5, 0, 0, 0, 5, 0, 0, 0);
            mockPriorityCounts(0, 0, 0, 0);

            DailySummaryDTO result = summaryService.getDailySummary(user);

            assertEquals(0.0, result.completionRate());
        }

        @Test
        @DisplayName("should return 100 completion rate when all completed")
        void shouldReturn100CompletionRateWhenAllCompleted() {
            mockAllCounts(10, 10, 0, 0, 0, 0, 0, 0);
            mockPriorityCounts(3, 4, 2, 1);

            DailySummaryDTO result = summaryService.getDailySummary(user);

            assertEquals(100.0, result.completionRate());
        }

        @Test
        @DisplayName("should return priority breakdown")
        void shouldReturnPriorityBreakdown() {
            mockAllCounts(20, 10, 5, 3, 2, 3, 2, 5);
            mockPriorityCounts(5, 8, 5, 2);

            DailySummaryDTO result = summaryService.getDailySummary(user);

            assertNotNull(result.byPriority());
            assertEquals(5L, result.byPriority().get("LOW"));
            assertEquals(8L, result.byPriority().get("MEDIUM"));
            assertEquals(5L, result.byPriority().get("HIGH"));
            assertEquals(2L, result.byPriority().get("CRITICAL"));
        }

        @Test
        @DisplayName("should return status breakdown")
        void shouldReturnStatusBreakdown() {
            mockAllCounts(20, 10, 5, 3, 2, 3, 2, 5);
            mockPriorityCounts(5, 8, 5, 2);

            DailySummaryDTO result = summaryService.getDailySummary(user);

            assertNotNull(result.byStatus());
            assertEquals(10L, result.byStatus().get("COMPLETED"));
            assertEquals(5L, result.byStatus().get("IN_PROGRESS"));
            assertEquals(3L, result.byStatus().get("NOT_STARTED"));
            assertEquals(2L, result.byStatus().get("CANCELLED"));
        }

        @Test
        @DisplayName("should handle empty repository")
        void shouldHandleEmptyRepository() {
            mockAllCounts(0, 0, 0, 0, 0, 0, 0, 0);
            mockPriorityCounts(0, 0, 0, 0);

            DailySummaryDTO result = summaryService.getDailySummary(user);

            assertEquals(0L, result.totalTodos());
            assertEquals(0L, result.completedCount());
            assertEquals(0.0, result.completionRate());
        }
    }

    // Helper methods to mock repository calls
    private void mockAllCounts(long total, long completed, long inProgress,
                               long notStarted, long cancelled,
                               long overdue, long dueToday, long upcoming) {
        when(todoRepository.countByUser(user)).thenReturn(total);
        mockStatusCounts(completed, inProgress, notStarted, cancelled);
        mockTimeCounts(overdue, dueToday, upcoming);
    }

    private void mockStatusCounts(long completed, long inProgress, long notStarted, long cancelled) {
        List<StatusCountProjection> statusResults = new ArrayList<>();
        if (completed > 0) statusResults.add(createStatusProjection(TaskStatus.COMPLETED, completed));
        if (inProgress > 0) statusResults.add(createStatusProjection(TaskStatus.IN_PROGRESS, inProgress));
        if (notStarted > 0) statusResults.add(createStatusProjection(TaskStatus.NOT_STARTED, notStarted));
        if (cancelled > 0) statusResults.add(createStatusProjection(TaskStatus.CANCELLED, cancelled));
        when(todoRepository.countGroupedByStatusAndUser(user)).thenReturn(statusResults);
    }

    private StatusCountProjection createStatusProjection(TaskStatus status, long count) {
        StatusCountProjection projection = mock(StatusCountProjection.class);
        when(projection.getStatus()).thenReturn(status);
        when(projection.getCount()).thenReturn(count);
        return projection;
    }

    private void mockTimeCounts(long overdue, long dueToday, long upcoming) {
        when(todoRepository.countOverdueByUser(eq(user), any(), any())).thenReturn(overdue);
        when(todoRepository.countDueBetweenByUser(eq(user), any(), any())).thenReturn(dueToday, upcoming);
    }

    private void mockPriorityCounts(long low, long medium, long high, long critical) {
        List<PriorityCountProjection> priorityResults = new ArrayList<>();
        if (low > 0) priorityResults.add(createPriorityProjection(TaskPriority.LOW, low));
        if (medium > 0) priorityResults.add(createPriorityProjection(TaskPriority.MEDIUM, medium));
        if (high > 0) priorityResults.add(createPriorityProjection(TaskPriority.HIGH, high));
        if (critical > 0) priorityResults.add(createPriorityProjection(TaskPriority.CRITICAL, critical));
        when(todoRepository.countGroupedByPriorityAndUser(user)).thenReturn(priorityResults);
    }

    private PriorityCountProjection createPriorityProjection(TaskPriority priority, long count) {
        PriorityCountProjection projection = mock(PriorityCountProjection.class);
        when(projection.getPriority()).thenReturn(priority);
        when(projection.getCount()).thenReturn(count);
        return projection;
    }
}
