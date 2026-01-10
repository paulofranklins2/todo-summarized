package org.duckdns.todosummarized.service;

import org.duckdns.todosummarized.config.AiProperties;
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
    private AiProperties aiProperties;

    @Mock
    private GeminiSummaryAdapter geminiAdapter;

    @InjectMocks
    private AiProviderSelector providerSelector;

    private DailySummaryDTO sampleMetrics;

    @BeforeEach
    void setUp() {
        // Default: AI is globally enabled with AUTO provider
        lenient().when(aiProperties.isEnabled()).thenReturn(true);
        lenient().when(aiProperties.getProvider()).thenReturn(AiProvider.AUTO);

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
            when(openAiAdapter.getModel()).thenReturn("gpt-5-nano");

            AiProviderSelector.AiGenerationResult result =
                    providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.OPENAI);

            assertTrue(result.success());
            assertEquals("OpenAI summary", result.summary());
            assertEquals("gpt-5-nano", result.model());
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
            when(geminiAdapter.getModel()).thenReturn("gemini-2.5-flash-lite");

            AiProviderSelector.AiGenerationResult result =
                    providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.GEMINI);

            assertTrue(result.success());
            assertEquals("Gemini summary", result.summary());
            assertEquals("gemini-2.5-flash-lite", result.model());
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
            when(openAiAdapter.getModel()).thenReturn("gpt-5-nano");

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
            when(geminiAdapter.getModel()).thenReturn("gemini-2.5-flash-lite");

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
            when(geminiAdapter.getModel()).thenReturn("gemini-2.5-flash-lite");

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
            assertEquals("All AI providers are disabled. Enable OpenAI or Gemini in configuration.", result.failureReason());
        }
    }

    @Nested
    @DisplayName("isAnyProviderAvailable")
    class IsAnyProviderAvailableTests {

        @Test
        @DisplayName("should return true when OpenAI is enabled")
        void shouldReturnTrueWhenOpenAiEnabled() {
            when(openAiAdapter.isEnabled()).thenReturn(true);

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
            when(openAiAdapter.getModel()).thenReturn("gpt-5-nano");
            when(geminiAdapter.isEnabled()).thenReturn(false);
            when(geminiAdapter.getModel()).thenReturn("gemini-2.5-flash-lite");

            AiProviderSelector.ProviderInfo[] info = providerSelector.getProviderInfo();

            assertEquals(2, info.length);
            assertEquals(AiProvider.OPENAI, info[0].provider());
            assertTrue(info[0].available());
            assertEquals("gpt-5-nano", info[0].model());
            assertEquals(AiProvider.GEMINI, info[1].provider());
            assertFalse(info[1].available());
            assertEquals("gemini-2.5-flash-lite", info[1].model());
        }

        @Test
        @DisplayName("should show providers as unavailable when AI is globally disabled")
        void shouldShowProvidersUnavailableWhenGloballyDisabled() {
            when(aiProperties.isEnabled()).thenReturn(false);
            when(openAiAdapter.getModel()).thenReturn("gpt-5-nano");
            when(geminiAdapter.getModel()).thenReturn("gemini-2.5-flash-lite");

            AiProviderSelector.ProviderInfo[] info = providerSelector.getProviderInfo();

            assertFalse(info[0].available());
            assertFalse(info[1].available());
        }
    }

    @Nested
    @DisplayName("Global AI disabled")
    class GlobalAiDisabledTests {

        @Test
        @DisplayName("should return failure when AI is globally disabled")
        void shouldReturnFailureWhenGloballyDisabled() {
            when(aiProperties.isEnabled()).thenReturn(false);

            AiProviderSelector.AiGenerationResult result =
                    providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.AUTO);

            assertFalse(result.success());
            assertEquals("AI-powered summary feature is disabled", result.failureReason());
            verify(openAiAdapter, never()).generateSummary(any(), any());
            verify(geminiAdapter, never()).generateSummary(any(), any());
        }

        @Test
        @DisplayName("should return no provider available when AI is globally disabled")
        void shouldReturnNoProviderAvailableWhenGloballyDisabled() {
            when(aiProperties.isEnabled()).thenReturn(false);

            assertFalse(providerSelector.isAnyProviderAvailable());
            assertFalse(providerSelector.isProviderAvailable(AiProvider.OPENAI));
            assertFalse(providerSelector.isProviderAvailable(AiProvider.GEMINI));
            assertFalse(providerSelector.isProviderAvailable(AiProvider.AUTO));
        }

        @Test
        @DisplayName("should return correct unavailable reason when AI is globally disabled")
        void shouldReturnCorrectUnavailableReasonWhenGloballyDisabled() {
            when(aiProperties.isEnabled()).thenReturn(false);

            String reason = providerSelector.getAggregatedUnavailableReason();

            assertEquals("AI-powered summary feature is disabled", reason);
        }
    }

    @Nested
    @DisplayName("Global provider preference")
    class GlobalProviderPreferenceTests {

        @Test
        @DisplayName("should use global provider when set to OPENAI and request is AUTO")
        void shouldUseGlobalProviderOpenAi() {
            when(aiProperties.getProvider()).thenReturn(AiProvider.OPENAI);
            when(openAiAdapter.isEnabled()).thenReturn(true);
            when(openAiAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER))
                    .thenReturn(Optional.of("OpenAI summary"));
            when(openAiAdapter.getModel()).thenReturn("gpt-5-nano");

            AiProviderSelector.AiGenerationResult result =
                    providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.AUTO);

            assertTrue(result.success());
            assertEquals(AiProvider.OPENAI, result.provider());
            verify(geminiAdapter, never()).isEnabled();
            verify(geminiAdapter, never()).generateSummary(any(), any());
        }

        @Test
        @DisplayName("should use global provider when set to GEMINI and request is AUTO")
        void shouldUseGlobalProviderGemini() {
            when(aiProperties.getProvider()).thenReturn(AiProvider.GEMINI);
            when(geminiAdapter.isEnabled()).thenReturn(true);
            when(geminiAdapter.generateSummary(sampleMetrics, SummaryType.DEVELOPER))
                    .thenReturn(Optional.of("Gemini summary"));
            when(geminiAdapter.getModel()).thenReturn("gemini-2.5-flash-lite");

            AiProviderSelector.AiGenerationResult result =
                    providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.AUTO);

            assertTrue(result.success());
            assertEquals(AiProvider.GEMINI, result.provider());
            verify(openAiAdapter, never()).isEnabled();
            verify(openAiAdapter, never()).generateSummary(any(), any());
        }
    }
}

