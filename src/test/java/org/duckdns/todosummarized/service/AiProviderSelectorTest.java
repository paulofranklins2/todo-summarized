package org.duckdns.todosummarized.service;

import org.duckdns.todosummarized.domains.enums.AiProvider;
import org.duckdns.todosummarized.domains.enums.SummaryType;
import org.duckdns.todosummarized.dto.DailySummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiProviderSelectorTest {

    @Mock
    private OpenAiSummaryAdapter openAiAdapter;

    @Mock
    private GeminiSummaryAdapter geminiAdapter;

    @InjectMocks
    private AiProviderSelector providerSelector;

    private DailySummaryDTO sampleMetrics;

    @BeforeEach
    void setUp() {
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
    @DisplayName("generateSummary with OPENAI provider")
    class OpenAiProviderTests {

        @Test
        @DisplayName("should return success when OpenAI succeeds")
        void shouldReturnSuccessWhenOpenAiSucceeds() {
            when(openAiAdapter.isEnabled()).thenReturn(true);
            when(openAiAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER))
                    .thenReturn(Optional.of("OpenAI summary"));
            when(openAiAdapter.getModel()).thenReturn("gpt-4o-mini");

            AiProviderSelector.AiGenerationResult result =
                    providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.OPENAI);

            assertTrue(result.success());
            assertEquals("OpenAI summary", result.summary());
            assertEquals("gpt-4o-mini", result.model());
            assertEquals(AiProvider.OPENAI, result.provider());
        }

        @Test
        @DisplayName("should return failure when OpenAI is disabled")
        void shouldReturnFailureWhenOpenAiDisabled() {
            when(openAiAdapter.isEnabled()).thenReturn(false);
            when(openAiAdapter.getUnavailableReason()).thenReturn("OpenAI is disabled");

            AiProviderSelector.AiGenerationResult result =
                    providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.OPENAI);

            assertFalse(result.success());
            assertEquals("OpenAI is disabled", result.failureReason());
        }
    }

    @Nested
    @DisplayName("generateSummary with GEMINI provider")
    class GeminiProviderTests {

        @Test
        @DisplayName("should return success when Gemini succeeds")
        void shouldReturnSuccessWhenGeminiSucceeds() {
            when(geminiAdapter.isEnabled()).thenReturn(true);
            when(geminiAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER))
                    .thenReturn(Optional.of("Gemini summary"));
            when(geminiAdapter.getModel()).thenReturn("gemini-1.5-flash");

            AiProviderSelector.AiGenerationResult result =
                    providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.GEMINI);

            assertTrue(result.success());
            assertEquals("Gemini summary", result.summary());
            assertEquals("gemini-1.5-flash", result.model());
            assertEquals(AiProvider.GEMINI, result.provider());
        }

        @Test
        @DisplayName("should return failure when Gemini is disabled")
        void shouldReturnFailureWhenGeminiDisabled() {
            when(geminiAdapter.isEnabled()).thenReturn(false);
            when(geminiAdapter.getUnavailableReason()).thenReturn("Gemini is disabled");

            AiProviderSelector.AiGenerationResult result =
                    providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.GEMINI);

            assertFalse(result.success());
            assertEquals("Gemini is disabled", result.failureReason());
        }
    }

    @Nested
    @DisplayName("generateSummary with AUTO provider")
    class AutoProviderTests {

        @Test
        @DisplayName("should use OpenAI when both are enabled and OpenAI succeeds")
        void shouldUseOpenAiWhenBothEnabledAndOpenAiSucceeds() {
            when(openAiAdapter.isEnabled()).thenReturn(true);
            when(openAiAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER))
                    .thenReturn(Optional.of("OpenAI summary"));
            when(openAiAdapter.getModel()).thenReturn("gpt-4o-mini");

            AiProviderSelector.AiGenerationResult result =
                    providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.AUTO);

            assertTrue(result.success());
            assertEquals("OpenAI summary", result.summary());
            assertEquals(AiProvider.OPENAI, result.provider());
            verify(geminiAdapter, never()).generateSummary(any(), any());
        }

        @Test
        @DisplayName("should fallback to Gemini when OpenAI fails")
        void shouldFallbackToGeminiWhenOpenAiFails() {
            when(openAiAdapter.isEnabled()).thenReturn(true);
            when(openAiAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER))
                    .thenReturn(Optional.empty());
            when(geminiAdapter.isEnabled()).thenReturn(true);
            when(geminiAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER))
                    .thenReturn(Optional.of("Gemini summary"));
            when(geminiAdapter.getModel()).thenReturn("gemini-1.5-flash");

            AiProviderSelector.AiGenerationResult result =
                    providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.AUTO);

            assertTrue(result.success());
            assertEquals("Gemini summary", result.summary());
            assertEquals(AiProvider.GEMINI, result.provider());
        }

        @Test
        @DisplayName("should use Gemini when OpenAI is disabled")
        void shouldUseGeminiWhenOpenAiDisabled() {
            when(openAiAdapter.isEnabled()).thenReturn(false);
            when(geminiAdapter.isEnabled()).thenReturn(true);
            when(geminiAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER))
                    .thenReturn(Optional.of("Gemini summary"));
            when(geminiAdapter.getModel()).thenReturn("gemini-1.5-flash");

            AiProviderSelector.AiGenerationResult result =
                    providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.AUTO);

            assertTrue(result.success());
            assertEquals("Gemini summary", result.summary());
            assertEquals(AiProvider.GEMINI, result.provider());
        }

        @Test
        @DisplayName("should return failure when both providers fail")
        void shouldReturnFailureWhenBothFail() {
            when(openAiAdapter.isEnabled()).thenReturn(true);
            when(openAiAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER))
                    .thenReturn(Optional.empty());
            when(geminiAdapter.isEnabled()).thenReturn(true);
            when(geminiAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER))
                    .thenReturn(Optional.empty());
            when(openAiAdapter.getUnavailableReason()).thenReturn("OpenAI error");
            when(geminiAdapter.getUnavailableReason()).thenReturn("Gemini error");

            AiProviderSelector.AiGenerationResult result =
                    providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.AUTO);

            assertFalse(result.success());
            assertNotNull(result.failureReason());
        }

        @Test
        @DisplayName("should return failure when both providers are disabled")
        void shouldReturnFailureWhenBothDisabled() {
            when(openAiAdapter.isEnabled()).thenReturn(false);
            when(geminiAdapter.isEnabled()).thenReturn(false);

            AiProviderSelector.AiGenerationResult result =
                    providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.AUTO);

            assertFalse(result.success());
            assertEquals("All AI providers are disabled", result.failureReason());
        }
    }

    @Nested
    @DisplayName("isAnyProviderAvailable")
    class IsAnyProviderAvailableTests {

        @Test
        @DisplayName("should return true when OpenAI is enabled")
        void shouldReturnTrueWhenOpenAiEnabled() {
            when(openAiAdapter.isEnabled()).thenReturn(true);
            when(geminiAdapter.isEnabled()).thenReturn(false);

            assertTrue(providerSelector.isAnyProviderAvailable());
        }

        @Test
        @DisplayName("should return true when Gemini is enabled")
        void shouldReturnTrueWhenGeminiEnabled() {
            when(openAiAdapter.isEnabled()).thenReturn(false);
            when(geminiAdapter.isEnabled()).thenReturn(true);

            assertTrue(providerSelector.isAnyProviderAvailable());
        }

        @Test
        @DisplayName("should return true when both are enabled")
        void shouldReturnTrueWhenBothEnabled() {
            when(openAiAdapter.isEnabled()).thenReturn(true);
            when(geminiAdapter.isEnabled()).thenReturn(true);

            assertTrue(providerSelector.isAnyProviderAvailable());
        }

        @Test
        @DisplayName("should return false when both are disabled")
        void shouldReturnFalseWhenBothDisabled() {
            when(openAiAdapter.isEnabled()).thenReturn(false);
            when(geminiAdapter.isEnabled()).thenReturn(false);

            assertFalse(providerSelector.isAnyProviderAvailable());
        }
    }

    @Nested
    @DisplayName("isProviderAvailable")
    class IsProviderAvailableTests {

        @Test
        @DisplayName("should check OpenAI availability for OPENAI provider")
        void shouldCheckOpenAiForOpenAiProvider() {
            when(openAiAdapter.isEnabled()).thenReturn(true);

            assertTrue(providerSelector.isProviderAvailable(AiProvider.OPENAI));
        }

        @Test
        @DisplayName("should check Gemini availability for GEMINI provider")
        void shouldCheckGeminiForGeminiProvider() {
            when(geminiAdapter.isEnabled()).thenReturn(true);

            assertTrue(providerSelector.isProviderAvailable(AiProvider.GEMINI));
        }

        @Test
        @DisplayName("should check any provider for AUTO")
        void shouldCheckAnyProviderForAuto() {
            when(openAiAdapter.isEnabled()).thenReturn(false);
            when(geminiAdapter.isEnabled()).thenReturn(true);

            assertTrue(providerSelector.isProviderAvailable(AiProvider.AUTO));
        }
    }

    @Nested
    @DisplayName("getProviderInfo")
    class GetProviderInfoTests {

        @Test
        @DisplayName("should return info for all providers")
        void shouldReturnInfoForAllProviders() {
            when(openAiAdapter.isEnabled()).thenReturn(true);
            when(openAiAdapter.getModel()).thenReturn("gpt-4o-mini");
            when(geminiAdapter.isEnabled()).thenReturn(false);
            when(geminiAdapter.getModel()).thenReturn("gemini-1.5-flash");

            AiProviderSelector.ProviderInfo[] info = providerSelector.getProviderInfo();

            assertEquals(2, info.length);
            assertEquals(AiProvider.OPENAI, info[0].provider());
            assertTrue(info[0].available());
            assertEquals("gpt-4o-mini", info[0].model());
            assertEquals(AiProvider.GEMINI, info[1].provider());
            assertFalse(info[1].available());
            assertEquals("gemini-1.5-flash", info[1].model());
        }
    }
}

