package org.duckdns.todosummarized.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.AiProvider;
import org.duckdns.todosummarized.domains.enums.SummaryType;
import org.duckdns.todosummarized.dto.AiSummaryDTO;
import org.duckdns.todosummarized.dto.DailySummaryDTO;
import org.duckdns.todosummarized.dto.SummaryTypeDTO;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Service for generating AI-powered summaries with automatic fallback to metrics-only.
 * Supports multiple AI providers (OpenAI, Gemini) with automatic failover.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummaryService {

    /**
     * Cached immutable list of summary type DTOs.
     * Created once at class load time since enum values are constant.
     */
    private static final List<SummaryTypeDTO> SUMMARY_TYPES = Arrays.stream(SummaryType.values())
            .map(SummaryTypeDTO::from)
            .toList();

    private final SummaryService summaryService;
    private final AiProviderSelector providerSelector;
    private final Clock clock;

    /**
     * Generates an AI-powered summary for the authenticated user using automatic provider selection.
     * Falls back to metrics-only if all AI providers are disabled or fail.
     *
     * @param user        the authenticated user
     * @param summaryType the type of summary to generate
     * @return AI summary with optional fallback to metrics
     */
    public AiSummaryDTO getAiSummary(User user, SummaryType summaryType) {
        return getAiSummary(user, summaryType, AiProvider.AUTO);
    }

    /**
     * Generates an AI-powered summary for the authenticated user using the specified provider.
     * Falls back to metrics-only if AI is disabled or fails.
     */
    public AiSummaryDTO getAiSummary(User user, SummaryType summaryType, AiProvider provider) {
        LocalDate today = LocalDate.now(clock);
        DailySummaryDTO metrics = summaryService.getDailySummary(user);

        if (!providerSelector.isProviderAvailable(provider)) {
            log.info("AI summary unavailable for provider {}, returning metrics fallback for user: {}",
                    provider, user.getUsername());
            return AiSummaryDTO.fallback(today, summaryType, providerSelector.getAggregatedUnavailableReason(), metrics);
        }

        AiProviderSelector.AiGenerationResult result = providerSelector.generateSummary(metrics, summaryType, provider);

        if (result.success()) {
            log.info("AI summary generated successfully for user: {}, type: {}, provider: {}",
                    user.getUsername(), summaryType, result.provider());
            return AiSummaryDTO.aiGenerated(today, summaryType, result.summary(), result.model(), metrics);
        } else {
            log.warn("AI summary failed, returning metrics fallback for user: {}. Reason: {}",
                    user.getUsername(), result.failureReason());
            return AiSummaryDTO.fallback(today, summaryType, result.failureReason(), metrics);
        }
    }

    /**
     * Returns all available summary type options.
     *
     * @return unmodifiable list of summary type DTOs
     */
    public List<SummaryTypeDTO> getAvailableSummaryTypes() {
        return SUMMARY_TYPES;
    }

    /**
     * Checks if any AI summary provider is currently available.
     */
    public boolean isAiAvailable() {
        return providerSelector.isAnyProviderAvailable();
    }

    /**
     * Gets information about available AI providers.
     */
    public AiProviderSelector.ProviderInfo[] getProviderInfo() {
        return providerSelector.getProviderInfo();
    }
}

