package org.duckdns.todosummarized.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.duckdns.todosummarized.config.GeminiProperties;
import org.duckdns.todosummarized.domains.enums.SummaryType;
import org.duckdns.todosummarized.dto.DailySummaryDTO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiSummaryAdapterTest {

    @Mock
    private GeminiProperties geminiProperties;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private AiSummaryMessageBuilder messageBuilder = new AiSummaryMessageBuilder();

    private GeminiSummaryAdapter geminiSummaryAdapter;

    private DailySummaryDTO sampleMetrics;

    @BeforeEach
    void setUp() {
        // Need to provide default timeout for HttpClient initialization
        when(geminiProperties.getTimeoutSeconds()).thenReturn(30);
        when(geminiProperties.getModel()).thenReturn("gemini-2.5-flash-lite");
        when(geminiProperties.isEnabled()).thenReturn(true);

        geminiSummaryAdapter = new GeminiSummaryAdapter(geminiProperties, objectMapper, messageBuilder);
        geminiSummaryAdapter.initHttpClient();

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

    @AfterEach
    void tearDown() {
        if (geminiSummaryAdapter != null) {
            geminiSummaryAdapter.destroyHttpClient();
        }
    }

    @Nested
    @DisplayName("isEnabled")
    class IsEnabledTests {

        @Test
        @DisplayName("should return true when Gemini is enabled")
        void shouldReturnTrueWhenEnabled() {
            when(geminiProperties.isEnabled()).thenReturn(true);

            assertTrue(geminiSummaryAdapter.isEnabled());
        }

        @Test
        @DisplayName("should return false when Gemini is disabled")
        void shouldReturnFalseWhenDisabled() {
            when(geminiProperties.isEnabled()).thenReturn(false);

            assertFalse(geminiSummaryAdapter.isEnabled());
        }
    }

    @Nested
    @DisplayName("getModel")
    class GetModelTests {

        @Test
        @DisplayName("should return configured model")
        void shouldReturnConfiguredModel() {
            when(geminiProperties.getModel()).thenReturn("gemini-2.5-flash-lite");

            assertEquals("gemini-2.5-flash-lite", geminiSummaryAdapter.getModel());
        }

        @Test
        @DisplayName("should return gemini-1.5-pro when configured")
        void shouldReturnGeminiProWhenConfigured() {
            when(geminiProperties.getModel()).thenReturn("gemini-1.5-pro");

            assertEquals("gemini-1.5-pro", geminiSummaryAdapter.getModel());
        }
    }

    @Nested
    @DisplayName("generateSummary")
    class GenerateSummaryTests {

        @Test
        @DisplayName("should return empty when Gemini is disabled")
        void shouldReturnEmptyWhenDisabled() {
            when(geminiProperties.isEnabled()).thenReturn(false);

            Optional<String> result = geminiSummaryAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty when API key is null")
        void shouldReturnEmptyWhenApiKeyNull() {
            when(geminiProperties.isEnabled()).thenReturn(true);
            when(geminiProperties.getApiKey()).thenReturn(null);

            Optional<String> result = geminiSummaryAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty when API key is blank")
        void shouldReturnEmptyWhenApiKeyBlank() {
            when(geminiProperties.isEnabled()).thenReturn(true);
            when(geminiProperties.getApiKey()).thenReturn("   ");

            Optional<String> result = geminiSummaryAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getUnavailableReason")
    class GetUnavailableReasonTests {

        @Test
        @DisplayName("should return disabled reason when Gemini is disabled")
        void shouldReturnDisabledReason() {
            when(geminiProperties.isEnabled()).thenReturn(false);

            String reason = geminiSummaryAdapter.getUnavailableReason();

            assertEquals("Gemini AI summary feature is disabled", reason);
        }

        @Test
        @DisplayName("should return API key reason when key is null")
        void shouldReturnApiKeyReasonWhenNull() {
            when(geminiProperties.isEnabled()).thenReturn(true);
            when(geminiProperties.getApiKey()).thenReturn(null);

            String reason = geminiSummaryAdapter.getUnavailableReason();

            assertEquals("Gemini API key is not configured", reason);
        }

        @Test
        @DisplayName("should return API key reason when key is blank")
        void shouldReturnApiKeyReasonWhenBlank() {
            when(geminiProperties.isEnabled()).thenReturn(true);
            when(geminiProperties.getApiKey()).thenReturn("");

            String reason = geminiSummaryAdapter.getUnavailableReason();

            assertEquals("Gemini API key is not configured", reason);
        }

        @Test
        @DisplayName("should return error reason when configured properly")
        void shouldReturnErrorReasonWhenConfigured() {
            when(geminiProperties.isEnabled()).thenReturn(true);
            when(geminiProperties.getApiKey()).thenReturn("AIza-test-key");

            String reason = geminiSummaryAdapter.getUnavailableReason();

            assertEquals("Gemini AI service encountered an error", reason);
        }
    }
}

