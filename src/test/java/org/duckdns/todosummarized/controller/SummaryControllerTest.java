package org.duckdns.todosummarized.controller;

import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.Role;
import org.duckdns.todosummarized.dto.DailySummaryDTO;
import org.duckdns.todosummarized.service.SummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryControllerTest {

    @Mock
    private SummaryService summaryService;

    @InjectMocks
    private SummaryController summaryController;

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

    @Test
    @DisplayName("getDailySummary returns 200 with summary data")
    void getDailySummary_returnsOkWithSummary() {
        DailySummaryDTO mockSummary = DailySummaryDTO.builder()
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
                .byPriority(Map.of("LOW", 5L, "MEDIUM", 10L, "HIGH", 8L, "CRITICAL", 2L))
                .byStatus(Map.of("COMPLETED", 10L, "IN_PROGRESS", 8L, "NOT_STARTED", 5L, "CANCELLED", 2L))
                .build();

        when(summaryService.getDailySummary(user)).thenReturn(mockSummary);

        ResponseEntity<DailySummaryDTO> response = summaryController.getDailySummary(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(25, response.getBody().totalTodos());
        assertEquals(10, response.getBody().completedCount());
        assertEquals(43.48, response.getBody().completionRate());
        verify(summaryService, times(1)).getDailySummary(user);
    }

    @Test
    @DisplayName("getDailySummary returns empty summary when no todos")
    void getDailySummary_returnsEmptySummaryWhenNoTodos() {
        DailySummaryDTO emptySummary = DailySummaryDTO.builder()
                .date(LocalDate.of(2026, 1, 9))
                .totalTodos(0)
                .completedCount(0)
                .inProgressCount(0)
                .notStartedCount(0)
                .cancelledCount(0)
                .overdueCount(0)
                .dueTodayCount(0)
                .upcomingCount(0)
                .completionRate(0.0)
                .byPriority(Map.of("LOW", 0L, "MEDIUM", 0L, "HIGH", 0L, "CRITICAL", 0L))
                .byStatus(Map.of("COMPLETED", 0L, "IN_PROGRESS", 0L, "NOT_STARTED", 0L, "CANCELLED", 0L))
                .build();

        when(summaryService.getDailySummary(user)).thenReturn(emptySummary);

        ResponseEntity<DailySummaryDTO> response = summaryController.getDailySummary(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().totalTodos());
        assertEquals(0.0, response.getBody().completionRate());
    }

    @Test
    @DisplayName("getDailySummary calls service exactly once")
    void getDailySummary_callsServiceOnce() {
        DailySummaryDTO mockSummary = DailySummaryDTO.builder()
                .date(LocalDate.now())
                .totalTodos(0)
                .completedCount(0)
                .inProgressCount(0)
                .notStartedCount(0)
                .cancelledCount(0)
                .overdueCount(0)
                .dueTodayCount(0)
                .upcomingCount(0)
                .completionRate(0.0)
                .byPriority(Map.of())
                .byStatus(Map.of())
                .build();

        when(summaryService.getDailySummary(user)).thenReturn(mockSummary);

        summaryController.getDailySummary(user);

        verify(summaryService, times(1)).getDailySummary(user);
        verifyNoMoreInteractions(summaryService);
    }
}

