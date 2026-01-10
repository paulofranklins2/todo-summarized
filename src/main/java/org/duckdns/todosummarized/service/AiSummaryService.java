package org.duckdns.todosummarized.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.duckdns.todosummarized.domains.entity.AiInsight;
import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.AiProvider;
import org.duckdns.todosummarized.domains.enums.SummaryType;
import org.duckdns.todosummarized.dto.AiSummaryDTO;
import org.duckdns.todosummarized.dto.DailySummaryDTO;
import org.duckdns.todosummarized.dto.SummaryTypeDTO;
import org.duckdns.todosummarized.repository.AiInsightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
    private final AiInsightRepository aiInsightRepository;
    private final CacheKeyBuilder cacheKeyBuilder;
    private final Clock clock;
    private final Cache<String, AiSummaryDTO> aiInsightCache;

    /**
     * Generates an AI-powered summary for the authenticated user using automatic provider selection.
     * Falls back to metrics-only if all AI providers are disabled or fail.
     */
    public AiSummaryDTO getAiSummary(User user, SummaryType summaryType) {
        return getAiSummary(user, summaryType, AiProvider.AUTO);
    }

    /**
     * Gets an AI insight for a user with cache-first strategy.
     * Otherwise generates a new insight and caches it.
     */
    @Transactional
    public AiSummaryDTO getAiSummary(User user, SummaryType summaryType, AiProvider provider) {
        // Check cache first
        Optional<AiSummaryDTO> cached = getCachedInsight(user);

        // Return cached if it exists AND matches the requested type
        if (cached.isPresent() && cached.get().summaryType() == summaryType) {
            log.debug("Returning cached insight for user: {}, type: {}", user.getUsername(), summaryType);
            return cached.get();
        }

        // Generate new insight (different type requested or no cache)
        return generateNewInsight(user, summaryType, provider);
    }

    /**
     * Gets the stored AI insight for a user, if available.
     */
    @Transactional(readOnly = true)
    public Optional<AiSummaryDTO> getCachedInsight(User user) {
        String cacheKey = cacheKeyBuilder.forAiInsight(user);

        // Check in-memory cache first
        AiSummaryDTO cached = aiInsightCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for AI insight, user: {}", user.getUsername());
            return Optional.of(cached);
        }

        // Fall back to database
        Optional<AiInsight> dbInsight = aiInsightRepository.findByUser(user);
        if (dbInsight.isPresent()) {
            log.debug("Database hit for AI insight, user: {}", user.getUsername());
            DailySummaryDTO metrics = summaryService.getDailySummary(user);
            AiSummaryDTO dto = convertToDTO(dbInsight.get(), metrics);
            // Populate the cache for future requests
            aiInsightCache.put(cacheKey, dto);
            return Optional.of(dto);
        }

        log.debug("No stored AI insight found for user: {}", user.getUsername());
        return Optional.empty();
    }

    /**
     * Generates a new AI insight for the user, replacing any existing stored insight.
     * Persists to database and updates the in-memory cache.
     * Use this when the user explicitly requests a new/different insight.
     */
    @Transactional
    public AiSummaryDTO generateNewInsight(User user, SummaryType summaryType, AiProvider provider) {
        AiSummaryDTO newInsight = generateAiSummaryInternal(user, summaryType, provider);

        // Save to database (replace existing if any)
        AiInsight entity = aiInsightRepository.findByUser(user)
                .orElseGet(() -> AiInsight.builder().user(user).build());

        updateEntityFromDTO(entity, newInsight, provider);
        aiInsightRepository.save(entity);

        // Update in-memory cache
        String cacheKey = cacheKeyBuilder.forAiInsight(user);
        aiInsightCache.put(cacheKey, newInsight);

        log.info("New AI insight generated and stored for user: {}, type: {}", user.getUsername(), summaryType);
        return newInsight;
    }

    /**
     * Invalidates the stored AI insight for a user.
     * Removes from both the database and in-memory cache.
     * Call this when user's todos change significantly.
     */
    @Transactional
    public void invalidateInsightCache(User user) {
        // Remove from database
        aiInsightRepository.findByUser(user)
                .ifPresent(insight -> aiInsightRepository.deleteByIdAndUser(insight.getId(), user));

        // Remove from in-memory cache
        String cacheKey = cacheKeyBuilder.forAiInsight(user);
        aiInsightCache.invalidate(cacheKey);

        log.debug("AI insight invalidated for user: {}", user.getUsername());
    }

    /**
     * Internal method that performs the actual AI summary generation without caching.
     */
    private AiSummaryDTO generateAiSummaryInternal(User user, SummaryType summaryType, AiProvider provider) {
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

    /**
     * Converts an AiInsight entity to AiSummaryDTO.
     */
    private AiSummaryDTO convertToDTO(AiInsight entity, DailySummaryDTO metrics) {
        if (entity.isAiGenerated()) {
            return AiSummaryDTO.aiGenerated(
                    entity.getSummaryDate(),
                    entity.getSummaryType(),
                    entity.getSummary(),
                    entity.getModel(),
                    metrics
            );
        } else {
            return AiSummaryDTO.fallback(
                    entity.getSummaryDate(),
                    entity.getSummaryType(),
                    entity.getFallbackReason(),
                    metrics
            );
        }
    }

    /**
     * Updates an AiInsight entity from an AiSummaryDTO.
     */
    private void updateEntityFromDTO(AiInsight entity, AiSummaryDTO dto, AiProvider provider) {
        entity.setSummaryType(dto.summaryType());
        entity.setProvider(provider);
        entity.setSummary(dto.summary());
        entity.setAiGenerated(dto.aiGenerated());
        entity.setFallbackReason(dto.fallbackReason());
        entity.setModel(dto.model());
        entity.setSummaryDate(dto.date());
    }
}

