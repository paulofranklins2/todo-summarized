package org.duckdns.todosummarized.service;

import lombok.RequiredArgsConstructor;
import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.TaskPriority;
import org.duckdns.todosummarized.domains.enums.TaskStatus;
import org.duckdns.todosummarized.dto.DailySummaryDTO;
import org.duckdns.todosummarized.repository.TodoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private static final int UPCOMING_DAYS_AHEAD = 8;
    private static final Set<TaskStatus> EXCLUDED_FROM_OVERDUE = EnumSet.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED);

    private final TodoRepository todoRepository;
    private final Clock clock;

    /**
     * Generates a daily summary of todos for the specified user with metrics and breakdowns.
     * Includes counts by status, priority, overdue items, and completion rate.
     *
     * @param user the authenticated user
     * @return a {@link DailySummaryDTO} containing today's todo metrics for the user
     */
    @Transactional(readOnly = true)
    public DailySummaryDTO getDailySummary(User user) {
        LocalDate today = LocalDate.now(clock);
        SummaryWindow summaryWindow = SummaryWindow.of(today, clock);

        Map<TaskStatus, Long> statusCounts = countsByStatus(user);
        Map<TaskPriority, Long> priorityCounts = countsByPriority(user);

        long totalTodos = todoRepository.countByUser(user);
        long cancelledCount = getOrZero(statusCounts, TaskStatus.CANCELLED);
        long completedCount = getOrZero(statusCounts, TaskStatus.COMPLETED);

        double completionRate = completionRate(totalTodos, completedCount, cancelledCount);

        return DailySummaryDTO.builder()
                .date(today)
                .totalTodos(totalTodos)
                .completedCount(completedCount)
                .inProgressCount(getOrZero(statusCounts, TaskStatus.IN_PROGRESS))
                .notStartedCount(getOrZero(statusCounts, TaskStatus.NOT_STARTED))
                .cancelledCount(cancelledCount)
                .overdueCount(todoRepository.countOverdueByUser(user, summaryWindow.now(), EXCLUDED_FROM_OVERDUE))
                .dueTodayCount(todoRepository.countDueBetweenByUser(user, summaryWindow.startOfDay(), summaryWindow.endOfDay()))
                .upcomingCount(todoRepository.countDueBetweenByUser(user, summaryWindow.startOfTomorrow(), summaryWindow.endOfUpcoming()))
                .completionRate(completionRate)
                .byPriority(toNameKeyedMap(priorityCounts))
                .byStatus(toNameKeyedMap(statusCounts))
                .build();
    }

    /**
     * Fetches todo counts grouped by status for the specified user in a single query.
     * Initializes all status values to 0 before populating from the database.
     *
     * @param user the user to filter by
     * @return a map of {@link TaskStatus} to count
     */
    private Map<TaskStatus, Long> countsByStatus(User user) {
        EnumMap<TaskStatus, Long> counts = initEnumCounts(TaskStatus.class, TaskStatus.values());
        todoRepository.countGroupedByStatusAndUser(user)
                .forEach(row -> counts.put(row.getStatus(), row.getCount()));
        return counts;
    }

    /**
     * Fetches todo counts grouped by priority for the specified user in a single query.
     * Initializes all priority values to 0 before populating from the database.
     *
     * @param user the user to filter by
     * @return a map of {@link TaskPriority} to count
     */
    private Map<TaskPriority, Long> countsByPriority(User user) {
        EnumMap<TaskPriority, Long> counts = initEnumCounts(TaskPriority.class, TaskPriority.values());
        todoRepository.countGroupedByPriorityAndUser(user)
                .forEach(row -> counts.put(row.getPriority(), row.getCount()));
        return counts;
    }

    /**
     * Initializes an EnumMap with all enum values set to zero.
     *
     * @param type   the enum class type
     * @param values all values of the enum
     * @param <E>    the enum type
     * @return an EnumMap with all keys initialized to 0L
     */
    private static <E extends Enum<E>> EnumMap<E, Long> initEnumCounts(Class<E> type, E[] values) {
        EnumMap<E, Long> counts = new EnumMap<>(type);
        for (E v : values) {
            counts.put(v, 0L);
        }
        return counts;
    }

    /**
     * Returns the value for the given key, or 0 if the key is not present.
     *
     * @param map the map to look up
     * @param key the key to find
     * @param <K> the key type
     * @return the value or 0L if absent
     */
    private static <K> long getOrZero(Map<K, Long> map, K key) {
        return map.getOrDefault(key, 0L);
    }

    /**
     * Calculates the completion rate as a percentage of active (non-cancelled) todos.
     * Returns 0.0 if there are no active todos.
     *
     * @param totalTodos     total number of todos
     * @param completedCount number of completed todos
     * @param cancelledCount number of cancelled todos
     * @return completion rate as a percentage (0.0 - 100.0), rounded to 2 decimal places
     */
    private static double completionRate(long totalTodos, long completedCount, long cancelledCount) {
        long activeTodos = Math.max(0, totalTodos - cancelledCount);
        double raw = (activeTodos == 0) ? 0.0 : (completedCount * 100.0 / activeTodos);
        return round2(raw);
    }

    /**
     * Converts an enum-keyed map to a string-keyed map using enum names.
     * Preserves insertion order using LinkedHashMap.
     *
     * @param enumKeyed the map with enum keys
     * @param <E>       the enum type
     * @return a new map with enum names as keys
     */
    private static <E extends Enum<E>> Map<String, Long> toNameKeyedMap(Map<E, Long> enumKeyed) {
        Map<String, Long> out = new LinkedHashMap<>();
        enumKeyed.forEach((k, v) -> out.put(k.name(), v));
        return out;
    }

    /**
     * Rounds a double value to 2 decimal places.
     *
     * @param value the value to round
     * @return the rounded value
     */
    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Immutable record representing the time boundaries for daily summary calculations.
     * Contains the current time and date range boundaries for today, tomorrow, and upcoming period.
     *
     * @param now             the current date-time
     * @param startOfDay      start of today (midnight)
     * @param endOfDay        end of today (23:59:59.999...)
     * @param startOfTomorrow start of tomorrow (midnight)
     * @param endOfUpcoming   end of the upcoming period (UPCOMING_DAYS_AHEAD days from today)
     */
    private record SummaryWindow(
            LocalDateTime now,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            LocalDateTime startOfTomorrow,
            LocalDateTime endOfUpcoming
    ) {
        /**
         * Factory method to create a SummaryWindow for the given date.
         *
         * @param today the date to calculate boundaries for
         * @param clock the clock to use for current time
         * @return a new SummaryWindow with calculated boundaries
         */
        static SummaryWindow of(LocalDate today, Clock clock) {
            return new SummaryWindow(
                    LocalDateTime.now(clock),
                    today.atStartOfDay(),
                    today.atTime(LocalTime.MAX),
                    today.plusDays(1).atStartOfDay(),
                    today.plusDays(UPCOMING_DAYS_AHEAD).atStartOfDay()
            );
        }
    }
}
