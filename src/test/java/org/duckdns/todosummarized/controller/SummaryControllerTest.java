package org.duckdns.todosummarized.controller;

import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.AiProvider;
import org.duckdns.todosummarized.domains.enums.Role;
import org.duckdns.todosummarized.domains.enums.SummaryType;
import org.duckdns.todosummarized.dto.AiSummaryDTO;
import org.duckdns.todosummarized.dto.DailySummaryDTO;
import org.duckdns.todosummarized.dto.SummaryTypeDTO;
import org.duckdns.todosummarized.service.AiProviderSelector;
import org.duckdns.todosummarized.service.AiSummaryService;
import org.duckdns.todosummarized.service.SummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryControllerTest {

    @Mock
    private SummaryService summaryService;

    @Mock
    private AiSummaryService aiSummaryService;

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

    @Nested
    @DisplayName("getAiSummary")
    class GetAiSummaryTests {

        @Test
        @DisplayName("returns 200 with AI-generated summary")
        void getAiSummary_returnsOkWithAiSummary() {
            DailySummaryDTO metrics = createSampleMetrics();
            AiSummaryDTO aiSummary = AiSummaryDTO.aiGenerated(
                    LocalDate.of(2026, 1, 9),
                    SummaryType.DEVELOPER,
                    "AI generated summary",
                    "gpt-5-nano",
                    metrics
            );

            when(aiSummaryService.getAiSummary(user, SummaryType.DEVELOPER, AiProvider.AUTO)).thenReturn(aiSummary);

            ResponseEntity<AiSummaryDTO> response = summaryController.getAiSummary(user, SummaryType.DEVELOPER, AiProvider.AUTO);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().aiGenerated());
            assertEquals("AI generated summary", response.getBody().summary());
        }

        @Test
        @DisplayName("returns 200 with fallback when AI disabled")
        void getAiSummary_returnsFallbackWhenDisabled() {
            DailySummaryDTO metrics = createSampleMetrics();
            AiSummaryDTO fallback = AiSummaryDTO.fallback(
                    LocalDate.of(2026, 1, 9),
                    SummaryType.EXECUTIVE,
                    "AI summary feature is disabled",
                    metrics
            );

            when(aiSummaryService.getAiSummary(user, SummaryType.EXECUTIVE, AiProvider.AUTO)).thenReturn(fallback);

            ResponseEntity<AiSummaryDTO> response = summaryController.getAiSummary(user, SummaryType.EXECUTIVE, AiProvider.AUTO);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertFalse(response.getBody().aiGenerated());
            assertEquals("AI summary feature is disabled", response.getBody().fallbackReason());
        }
    }

    @Nested
    @DisplayName("getSummaryTypes")
    class GetSummaryTypesTests {

        @Test
        @DisplayName("returns 200 with all summary types")
        void getSummaryTypes_returnsAllTypes() {
            List<SummaryTypeDTO> types = List.of(
                    SummaryTypeDTO.from(SummaryType.DEVELOPER),
                    SummaryTypeDTO.from(SummaryType.EXECUTIVE)
            );

            when(aiSummaryService.getAvailableSummaryTypes()).thenReturn(types);

            ResponseEntity<List<SummaryTypeDTO>> response = summaryController.getSummaryTypes();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(2, response.getBody().size());
        }
    }

    @Nested
    @DisplayName("getAiStatus")
    class GetAiStatusTests {

        @Test
        @DisplayName("returns 200 with available true when AI is enabled")
        void getAiStatus_returnsAvailableTrue() {
            when(aiSummaryService.isAiAvailable()).thenReturn(true);
            when(aiSummaryService.getProviderInfo()).thenReturn(new AiProviderSelector.ProviderInfo[]{
                    new AiProviderSelector.ProviderInfo(AiProvider.OPENAI, true, "gpt-5-nano"),
                    new AiProviderSelector.ProviderInfo(AiProvider.GEMINI, false, "gemini-2.5-flash-lite")
            });

            ResponseEntity<Map<String, Object>> response = summaryController.getAiStatus();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue((Boolean) response.getBody().get("available"));
        }

        @Test
        @DisplayName("returns 200 with available false when AI is disabled")
        void getAiStatus_returnsAvailableFalse() {
            when(aiSummaryService.isAiAvailable()).thenReturn(false);
            when(aiSummaryService.getProviderInfo()).thenReturn(new AiProviderSelector.ProviderInfo[]{
                    new AiProviderSelector.ProviderInfo(AiProvider.OPENAI, false, "gpt-5-nano"),
                    new AiProviderSelector.ProviderInfo(AiProvider.GEMINI, false, "gemini-2.5-flash-lite")
            });

            ResponseEntity<Map<String, Object>> response = summaryController.getAiStatus();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertFalse((Boolean) response.getBody().get("available"));
        }
    }

    private DailySummaryDTO createSampleMetrics() {
        return DailySummaryDTO.builder()
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
    }
}

