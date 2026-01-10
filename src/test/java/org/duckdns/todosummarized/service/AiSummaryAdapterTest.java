package org.duckdns.todosummarized.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.duckdns.todosummarized.config.OpenAiProperties;
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
class AiSummaryAdapterTest {

    @Mock
    private OpenAiProperties openAiProperties;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private AiSummaryMessageBuilder messageBuilder = new AiSummaryMessageBuilder();

    private OpenAiSummaryAdapter aiSummaryAdapter;

    private DailySummaryDTO sampleMetrics;

    @BeforeEach
    void setUp() {
        // Need to provide default timeout for HttpClient initialization
        when(openAiProperties.getTimeoutSeconds()).thenReturn(30);
        when(openAiProperties.getModel()).thenReturn("gpt-4o-mini");
        when(openAiProperties.isEnabled()).thenReturn(true);

        aiSummaryAdapter = new OpenAiSummaryAdapter(openAiProperties, objectMapper, messageBuilder);
        aiSummaryAdapter.initHttpClient();

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
        if (aiSummaryAdapter != null) {
            aiSummaryAdapter.destroyHttpClient();
        }
    }

    @Nested
    @DisplayName("isEnabled")
    class IsEnabledTests {

        @Test
        @DisplayName("should return true when AI is enabled")
        void shouldReturnTrueWhenEnabled() {
            when(openAiProperties.isEnabled()).thenReturn(true);

            assertTrue(aiSummaryAdapter.isEnabled());
        }

        @Test
        @DisplayName("should return false when AI is disabled")
        void shouldReturnFalseWhenDisabled() {
            when(openAiProperties.isEnabled()).thenReturn(false);

            assertFalse(aiSummaryAdapter.isEnabled());
        }
    }

    @Nested
    @DisplayName("getModel")
    class GetModelTests {

        @Test
        @DisplayName("should return configured model")
        void shouldReturnConfiguredModel() {
            when(openAiProperties.getModel()).thenReturn("gpt-4o-mini");

            assertEquals("gpt-4o-mini", aiSummaryAdapter.getModel());
        }
    }

    @Nested
    @DisplayName("generateSummary")
    class GenerateSummaryTests {

        @Test
        @DisplayName("should return empty when AI is disabled")
        void shouldReturnEmptyWhenDisabled() {
            when(openAiProperties.isEnabled()).thenReturn(false);

            Optional<String> result = aiSummaryAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty when API key is null")
        void shouldReturnEmptyWhenApiKeyNull() {
            when(openAiProperties.isEnabled()).thenReturn(true);
            when(openAiProperties.getApiKey()).thenReturn(null);

            Optional<String> result = aiSummaryAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty when API key is blank")
        void shouldReturnEmptyWhenApiKeyBlank() {
            when(openAiProperties.isEnabled()).thenReturn(true);
            when(openAiProperties.getApiKey()).thenReturn("   ");

            Optional<String> result = aiSummaryAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getUnavailableReason")
    class GetUnavailableReasonTests {

        @Test
        @DisplayName("should return disabled reason when AI is disabled")
        void shouldReturnDisabledReason() {
            when(openAiProperties.isEnabled()).thenReturn(false);

            String reason = aiSummaryAdapter.getUnavailableReason();

            assertEquals("OpenAI AI summary feature is disabled", reason);
        }

        @Test
        @DisplayName("should return API key reason when key is null")
        void shouldReturnApiKeyReasonWhenNull() {
            when(openAiProperties.isEnabled()).thenReturn(true);
            when(openAiProperties.getApiKey()).thenReturn(null);

            String reason = aiSummaryAdapter.getUnavailableReason();

            assertEquals("OpenAI API key is not configured", reason);
        }

        @Test
        @DisplayName("should return API key reason when key is blank")
        void shouldReturnApiKeyReasonWhenBlank() {
            when(openAiProperties.isEnabled()).thenReturn(true);
            when(openAiProperties.getApiKey()).thenReturn("");

            String reason = aiSummaryAdapter.getUnavailableReason();

            assertEquals("OpenAI API key is not configured", reason);
        }

        @Test
        @DisplayName("should return error reason when configured properly")
        void shouldReturnErrorReasonWhenConfigured() {
            when(openAiProperties.isEnabled()).thenReturn(true);
            when(openAiProperties.getApiKey()).thenReturn("sk-test-key");

            String reason = aiSummaryAdapter.getUnavailableReason();

            assertEquals("OpenAI AI service encountered an error", reason);
        }
    }
}

