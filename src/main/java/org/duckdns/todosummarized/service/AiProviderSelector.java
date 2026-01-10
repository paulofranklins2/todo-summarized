package org.duckdns.todosummarized.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.duckdns.todosummarized.config.AiProperties;
import org.duckdns.todosummarized.domains.enums.AiProvider;
import org.duckdns.todosummarized.domains.enums.SummaryType;
import org.duckdns.todosummarized.dto.DailySummaryDTO;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for selecting and coordinating between multiple AI providers.
 * Supports automatic fallback from one provider to another.
 * Respects global AI configuration to skip unnecessary provider checks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiProviderSelector {

    private final AiProperties aiProperties;
    private final OpenAiSummaryAdapter openAiAdapter;
    private final GeminiSummaryAdapter geminiAdapter;

    @PostConstruct
    void logConfiguration() {
        if (!aiProperties.isEnabled()) {
            log.info("AI Summary feature is globally DISABLED");
        } else {
            log.info("AI Summary feature enabled with preferred provider: {}", aiProperties.getProvider());
        }
    }

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
     * Generates an AI summary using the configured provider.
     * Uses the global preferred provider setting, but can be overridden.
     *
     * @param metrics         the daily summary metrics
     * @param summaryType     the type of summary to generate
     * @param preferredProvider the preferred AI provider (or AUTO for automatic selection)
     * @return the generation result with summary and provider info
     */
    public AiGenerationResult generateSummary(DailySummaryDTO metrics, SummaryType summaryType, AiProvider preferredProvider) {
        // Check if AI is globally disabled
        if (!aiProperties.isEnabled()) {
            return AiGenerationResult.failure("AI-powered summary feature is disabled");
        }

        // Use global preferred provider if AUTO is passed and global setting is not AUTO
        AiProvider effectiveProvider = preferredProvider;
        if (preferredProvider == AiProvider.AUTO && aiProperties.getProvider() != AiProvider.AUTO) {
            effectiveProvider = aiProperties.getProvider();
            log.debug("Using globally configured provider: {}", effectiveProvider);
        }

        return switch (effectiveProvider) {
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
     * Takes into account the global AI enabled setting.
     *
     * @return true if AI is globally enabled and at least one provider is enabled
     */
    public boolean isAnyProviderAvailable() {
        if (!aiProperties.isEnabled()) {
            return false;
        }
        return openAiAdapter.isEnabled() || geminiAdapter.isEnabled();
    }

    /**
     * Checks if a specific provider is available.
     * Takes into account the global AI enabled setting.
     *
     * @param provider the provider to check
     * @return true if the provider is enabled and configured
     */
    public boolean isProviderAvailable(AiProvider provider) {
        if (!aiProperties.isEnabled()) {
            return false;
        }
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
        if (!aiProperties.isEnabled()) {
            return "AI-powered summary feature is disabled";
        }
        if (!openAiAdapter.isEnabled() && !geminiAdapter.isEnabled()) {
            return "All AI providers are disabled. Enable OpenAI or Gemini in configuration.";
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
        boolean globalEnabled = aiProperties.isEnabled();
        return new ProviderInfo[]{
                new ProviderInfo(AiProvider.OPENAI, globalEnabled && openAiAdapter.isEnabled(), openAiAdapter.getModel()),
                new ProviderInfo(AiProvider.GEMINI, globalEnabled && geminiAdapter.isEnabled(), geminiAdapter.getModel())
        };
    }

    /**
     * Gets the globally configured preferred provider.
     *
     * @return the configured AI provider preference
     */
    public AiProvider getConfiguredProvider() {
        return aiProperties.getProvider();
    }

    /**
     * Checks if AI is globally enabled.
     *
     * @return true if AI feature is globally enabled
     */
    public boolean isGloballyEnabled() {
        return aiProperties.isEnabled();
    }

    /**
     * Information about a single AI provider.
     */
    public record ProviderInfo(AiProvider provider, boolean available, String model) {}
}

