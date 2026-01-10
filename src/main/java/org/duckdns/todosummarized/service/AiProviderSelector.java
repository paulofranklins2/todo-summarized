package org.duckdns.todosummarized.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.duckdns.todosummarized.domains.enums.AiProvider;
import org.duckdns.todosummarized.domains.enums.SummaryType;
import org.duckdns.todosummarized.dto.DailySummaryDTO;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for selecting and coordinating between multiple AI providers.
 * Supports automatic fallback from one provider to another.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiProviderSelector {

    private final OpenAiSummaryAdapter openAiAdapter;
    private final GeminiSummaryAdapter geminiAdapter;

    /**
     * Result of an AI generation attempt, including provider info.
     */
    public record AiGenerationResult(
            String summary,
            String model,
            AiProvider provider,
            boolean success,
            String failureReason
    ) {
        public static AiGenerationResult success(String summary, String model, AiProvider provider) {
            return new AiGenerationResult(summary, model, provider, true, null);
        }

        public static AiGenerationResult failure(String reason) {
            return new AiGenerationResult(null, null, null, false, reason);
        }
    }

    /**
     * Generates an AI summary using the specified provider, with automatic fallback if AUTO is selected.
     *
     * @param metrics         the daily summary metrics
     * @param summaryType     the type of summary to generate
     * @param preferredProvider the preferred AI provider (or AUTO for automatic selection)
     * @return the generation result with summary and provider info
     */
    public AiGenerationResult generateSummary(DailySummaryDTO metrics, SummaryType summaryType, AiProvider preferredProvider) {
        return switch (preferredProvider) {
            case OPENAI -> tryOpenAi(metrics, summaryType);
            case GEMINI -> tryGemini(metrics, summaryType);
            case AUTO -> tryAutoSelect(metrics, summaryType);
        };
    }

    /**
     * Tries OpenAI as the primary provider.
     */
    private AiGenerationResult tryOpenAi(DailySummaryDTO metrics, SummaryType summaryType) {
        if (!openAiAdapter.isEnabled()) {
            return AiGenerationResult.failure(openAiAdapter.getUnavailableReason());
        }

        Optional<String> result = openAiAdapter.generateSummary(metrics, summaryType);
        if (result.isPresent()) {
            return AiGenerationResult.success(result.get(), openAiAdapter.getModel(), AiProvider.OPENAI);
        }
        return AiGenerationResult.failure(openAiAdapter.getUnavailableReason());
    }

    /**
     * Tries Gemini as the primary provider.
     */
    private AiGenerationResult tryGemini(DailySummaryDTO metrics, SummaryType summaryType) {
        if (!geminiAdapter.isEnabled()) {
            return AiGenerationResult.failure(geminiAdapter.getUnavailableReason());
        }

        Optional<String> result = geminiAdapter.generateSummary(metrics, summaryType);
        if (result.isPresent()) {
            return AiGenerationResult.success(result.get(), geminiAdapter.getModel(), AiProvider.GEMINI);
        }
        return AiGenerationResult.failure(geminiAdapter.getUnavailableReason());
    }

    /**
     * Automatically selects the best available provider.
     * Priority: OpenAI first, then Gemini as fallback.
     */
    private AiGenerationResult tryAutoSelect(DailySummaryDTO metrics, SummaryType summaryType) {
        // Try OpenAI first if enabled
        if (openAiAdapter.isEnabled()) {
            log.debug("AUTO mode: Trying OpenAI first");
            Optional<String> openAiResult = openAiAdapter.generateSummary(metrics, summaryType);
            if (openAiResult.isPresent()) {
                log.info("AUTO mode: OpenAI succeeded");
                return AiGenerationResult.success(openAiResult.get(), openAiAdapter.getModel(), AiProvider.OPENAI);
            }
            log.warn("AUTO mode: OpenAI failed, trying Gemini fallback");
        }

        // Try Gemini as fallback
        if (geminiAdapter.isEnabled()) {
            log.debug("AUTO mode: Trying Gemini");
            Optional<String> geminiResult = geminiAdapter.generateSummary(metrics, summaryType);
            if (geminiResult.isPresent()) {
                log.info("AUTO mode: Gemini succeeded");
                return AiGenerationResult.success(geminiResult.get(), geminiAdapter.getModel(), AiProvider.GEMINI);
            }
            log.warn("AUTO mode: Gemini also failed");
        }

        // Both failed
        return AiGenerationResult.failure(getAggregatedUnavailableReason());
    }

    /**
     * Checks if any AI provider is currently available.
     *
     * @return true if at least one provider is enabled and configured
     */
    public boolean isAnyProviderAvailable() {
        return openAiAdapter.isEnabled() || geminiAdapter.isEnabled();
    }

    /**
     * Checks if a specific provider is available.
     *
     * @param provider the provider to check
     * @return true if the provider is enabled and configured
     */
    public boolean isProviderAvailable(AiProvider provider) {
        return switch (provider) {
            case OPENAI -> openAiAdapter.isEnabled();
            case GEMINI -> geminiAdapter.isEnabled();
            case AUTO -> isAnyProviderAvailable();
        };
    }

    /**
     * Returns the aggregated reason why AI is unavailable.
     */
    public String getAggregatedUnavailableReason() {
        if (!openAiAdapter.isEnabled() && !geminiAdapter.isEnabled()) {
            return "All AI providers are disabled";
        }
        if (!openAiAdapter.isEnabled()) {
            return "OpenAI disabled; Gemini: " + geminiAdapter.getUnavailableReason();
        }
        if (!geminiAdapter.isEnabled()) {
            return "OpenAI: " + openAiAdapter.getUnavailableReason() + "; Gemini disabled";
        }
        return "AI service encountered an error";
    }

    /**
     * Gets information about available providers for the API.
     *
     * @return array of provider availability info
     */
    public ProviderInfo[] getProviderInfo() {
        return new ProviderInfo[]{
                new ProviderInfo(AiProvider.OPENAI, openAiAdapter.isEnabled(), openAiAdapter.getModel()),
                new ProviderInfo(AiProvider.GEMINI, geminiAdapter.isEnabled(), geminiAdapter.getModel())
        };
    }

    /**
     * Information about a single AI provider.
     */
    public record ProviderInfo(AiProvider provider, boolean available, String model) {}
}

