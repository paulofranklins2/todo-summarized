package org.duckdns.todosummarized.service;

import com.github.benmanes.caffeine.cache.Cache;
import org.duckdns.todosummarized.domains.entity.AiInsight;
import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.AiProvider;
import org.duckdns.todosummarized.domains.enums.Role;
import org.duckdns.todosummarized.domains.enums.SummaryType;
import org.duckdns.todosummarized.dto.AiSummaryDTO;
import org.duckdns.todosummarized.dto.DailySummaryDTO;
import org.duckdns.todosummarized.dto.SummaryTypeDTO;
import org.duckdns.todosummarized.repository.AiInsightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiSummaryServiceTest {

    @Mock
    private SummaryService summaryService;

    @Mock
    private AiProviderSelector providerSelector;

    @Mock
    private AiInsightRepository aiInsightRepository;

    @Mock
    private CacheKeyBuilder cacheKeyBuilder;

    @Mock
    private Clock clock;

    @Mock
    private Cache<String, AiSummaryDTO> aiInsightCache;

    private AiSummaryService aiSummaryService;

    private User user;
    private DailySummaryDTO sampleMetrics;
    private String userCacheKey;

    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 1, 9);
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    @BeforeEach
    void setUp() {
        Instant fixedInstant = FIXED_DATE.atStartOfDay(ZONE_ID).toInstant();
        lenient().when(clock.instant()).thenReturn(fixedInstant);
        lenient().when(clock.getZone()).thenReturn(ZONE_ID);

        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("password")
                .role(Role.ROLE_USER)
                .build();

        // Set up cache key for this user
        userCacheKey = "ai-insight:" + user.getId().toString();
        lenient().when(cacheKeyBuilder.forAiInsight(any(User.class)))
                .thenAnswer(invocation -> {
                    User u = invocation.getArgument(0);
                    return "ai-insight:" + u.getId().toString();
                });

        // Manually construct the service with all mocks
        aiSummaryService = new AiSummaryService(
                summaryService,
                providerSelector,
                aiInsightRepository,
                cacheKeyBuilder,
                clock,
                aiInsightCache
        );


        sampleMetrics = DailySummaryDTO.builder()
                .date(FIXED_DATE)
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
    @DisplayName("getAiSummary")
    class GetAiSummaryTests {

        @Test
        @DisplayName("should return AI-generated summary when AI is enabled and succeeds")
        void shouldReturnAiGeneratedSummary() {
            when(summaryService.getDailySummary(user)).thenReturn(sampleMetrics);
            when(providerSelector.isProviderAvailable(AiProvider.AUTO)).thenReturn(true);
            when(providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.AUTO))
                    .thenReturn(AiProviderSelector.AiGenerationResult.success(
                            "AI generated summary text", "gpt-5-nano", AiProvider.OPENAI));
            when(aiInsightRepository.findByUser(user)).thenReturn(Optional.empty());

            AiSummaryDTO result = aiSummaryService.getAiSummary(user, SummaryType.DEVELOPER);

            assertTrue(result.aiGenerated());
            assertEquals("AI generated summary text", result.summary());
            assertEquals(SummaryType.DEVELOPER, result.summaryType());
            assertEquals("Software Engineer / Developer", result.summaryTypeName());
            assertEquals("gpt-5-nano", result.model());
            assertNull(result.fallbackReason());
            assertNotNull(result.metrics());
            verify(aiInsightRepository).save(any(AiInsight.class));
        }

        @Test
        @DisplayName("should return fallback when AI is disabled")
        void shouldReturnFallbackWhenDisabled() {
            when(summaryService.getDailySummary(user)).thenReturn(sampleMetrics);
            when(providerSelector.isProviderAvailable(AiProvider.AUTO)).thenReturn(false);
            when(providerSelector.getAggregatedUnavailableReason()).thenReturn("All AI providers are disabled");
            when(aiInsightRepository.findByUser(user)).thenReturn(Optional.empty());

            AiSummaryDTO result = aiSummaryService.getAiSummary(user, SummaryType.EXECUTIVE);

            assertFalse(result.aiGenerated());
            assertNull(result.summary());
            assertNull(result.model());
            assertEquals("All AI providers are disabled", result.fallbackReason());
            assertEquals(SummaryType.EXECUTIVE, result.summaryType());
            assertNotNull(result.metrics());
        }

        @Test
        @DisplayName("should return fallback when AI generation fails")
        void shouldReturnFallbackWhenGenerationFails() {
            when(summaryService.getDailySummary(user)).thenReturn(sampleMetrics);
            when(providerSelector.isProviderAvailable(AiProvider.AUTO)).thenReturn(true);
            when(providerSelector.generateSummary(sampleMetrics, SummaryType.STUDENT, AiProvider.AUTO))
                    .thenReturn(AiProviderSelector.AiGenerationResult.failure("AI service encountered an error"));
            when(aiInsightRepository.findByUser(user)).thenReturn(Optional.empty());

            AiSummaryDTO result = aiSummaryService.getAiSummary(user, SummaryType.STUDENT);

            assertFalse(result.aiGenerated());
            assertNull(result.summary());
            assertEquals("AI service encountered an error", result.fallbackReason());
        }

        @Test
        @DisplayName("should include date in response")
        void shouldIncludeDateInResponse() {
            when(summaryService.getDailySummary(user)).thenReturn(sampleMetrics);
            when(providerSelector.isProviderAvailable(AiProvider.AUTO)).thenReturn(true);
            when(providerSelector.generateSummary(any(), any(), any()))
                    .thenReturn(AiProviderSelector.AiGenerationResult.success(
                            "Summary", "gpt-5-nano", AiProvider.OPENAI));
            when(aiInsightRepository.findByUser(user)).thenReturn(Optional.empty());

            AiSummaryDTO result = aiSummaryService.getAiSummary(user, SummaryType.DEVELOPER);

            assertEquals(FIXED_DATE, result.date());
        }

        @Test
        @DisplayName("should return cached insight when type matches")
        void shouldReturnCachedInsightWhenTypeMatches() {
            AiSummaryDTO cachedInsight = AiSummaryDTO.aiGenerated(
                    FIXED_DATE, SummaryType.DEVELOPER, "Cached summary", "gpt-5-nano", sampleMetrics);
            when(aiInsightCache.getIfPresent(userCacheKey)).thenReturn(cachedInsight);

            AiSummaryDTO result = aiSummaryService.getAiSummary(user, SummaryType.DEVELOPER);

            assertEquals("Cached summary", result.summary());
            assertEquals(SummaryType.DEVELOPER, result.summaryType());
            // Should NOT call AI provider or save to repository
            verify(providerSelector, never()).generateSummary(any(), any(), any());
            verify(aiInsightRepository, never()).save(any());
        }

        @Test
        @DisplayName("should generate new insight when cached type differs from requested")
        void shouldGenerateNewInsightWhenTypeDiffers() {
            // Cache has DEVELOPER type
            AiSummaryDTO cachedInsight = AiSummaryDTO.aiGenerated(
                    FIXED_DATE, SummaryType.DEVELOPER, "Cached developer summary", "gpt-5-nano", sampleMetrics);
            when(aiInsightCache.getIfPresent(userCacheKey)).thenReturn(cachedInsight);

            // But user requests EXECUTIVE type
            when(summaryService.getDailySummary(user)).thenReturn(sampleMetrics);
            when(providerSelector.isProviderAvailable(AiProvider.AUTO)).thenReturn(true);
            when(providerSelector.generateSummary(sampleMetrics, SummaryType.EXECUTIVE, AiProvider.AUTO))
                    .thenReturn(AiProviderSelector.AiGenerationResult.success(
                            "New executive summary", "gpt-5-nano", AiProvider.OPENAI));
            when(aiInsightRepository.findByUser(user)).thenReturn(Optional.empty());

            AiSummaryDTO result = aiSummaryService.getAiSummary(user, SummaryType.EXECUTIVE);

            assertEquals("New executive summary", result.summary());
            assertEquals(SummaryType.EXECUTIVE, result.summaryType());
            // Should generate new and save
            verify(aiInsightRepository).save(any(AiInsight.class));
        }
    }

    @Nested
    @DisplayName("getAvailableSummaryTypes")
    class GetAvailableSummaryTypesTests {

        @Test
        @DisplayName("should return all summary types")
        void shouldReturnAllSummaryTypes() {
            List<SummaryTypeDTO> result = aiSummaryService.getAvailableSummaryTypes();

            assertEquals(SummaryType.values().length, result.size());
        }

        @Test
        @DisplayName("should include all expected summary types")
        void shouldIncludeAllExpectedTypes() {
            List<SummaryTypeDTO> result = aiSummaryService.getAvailableSummaryTypes();

            List<String> values = result.stream().map(SummaryTypeDTO::value).toList();

            assertTrue(values.contains("EXECUTIVE"));
            assertTrue(values.contains("DEVELOPER"));
            assertTrue(values.contains("STUDENT"));
            assertTrue(values.contains("FOCUS_SUPPORT"));
            assertTrue(values.contains("CREATIVE"));
            assertTrue(values.contains("OPERATIONS"));
            assertTrue(values.contains("PERSONAL"));
            assertTrue(values.contains("STANDUP"));
            assertTrue(values.contains("WEEKLY_REVIEW"));
            assertTrue(values.contains("MINIMAL"));
        }

        @Test
        @DisplayName("should include display names and descriptions")
        void shouldIncludeDisplayNamesAndDescriptions() {
            List<SummaryTypeDTO> result = aiSummaryService.getAvailableSummaryTypes();

            SummaryTypeDTO developer = result.stream()
                    .filter(t -> t.value().equals("DEVELOPER"))
                    .findFirst()
                    .orElseThrow();

            assertEquals("Software Engineer / Developer", developer.displayName());
            assertEquals("Structured, technical, standup-ready.", developer.description());
        }
    }

    @Nested
    @DisplayName("isAiAvailable")
    class IsAiAvailableTests {

        @Test
        @DisplayName("should return true when AI is enabled")
        void shouldReturnTrueWhenEnabled() {
            when(providerSelector.isAnyProviderAvailable()).thenReturn(true);

            assertTrue(aiSummaryService.isAiAvailable());
        }

        @Test
        @DisplayName("should return false when AI is disabled")
        void shouldReturnFalseWhenDisabled() {
            when(providerSelector.isAnyProviderAvailable()).thenReturn(false);

            assertFalse(aiSummaryService.isAiAvailable());
        }
    }

    @Nested
    @DisplayName("getCachedInsight")
    class GetCachedInsightTests {

        @Test
        @DisplayName("should return cached insight from memory when available")
        void shouldReturnCachedInsightFromMemoryWhenAvailable() {
            AiSummaryDTO cachedInsight = AiSummaryDTO.aiGenerated(
                    FIXED_DATE, SummaryType.DEVELOPER, "Cached summary", "gpt-5-nano", sampleMetrics);
            when(aiInsightCache.getIfPresent(userCacheKey)).thenReturn(cachedInsight);

            Optional<AiSummaryDTO> result = aiSummaryService.getCachedInsight(user);

            assertTrue(result.isPresent());
            assertEquals("Cached summary", result.get().summary());
            verify(aiInsightRepository, never()).findByUser(any());
        }

        @Test
        @DisplayName("should fall back to database when not in memory cache")
        void shouldFallBackToDatabaseWhenNotInMemory() {
            when(aiInsightCache.getIfPresent(userCacheKey)).thenReturn(null);

            AiInsight dbInsight = AiInsight.builder()
                    .user(user)
                    .summaryType(SummaryType.DEVELOPER)
                    .provider(AiProvider.OPENAI)
                    .summary("DB summary")
                    .aiGenerated(true)
                    .model("gpt-5-nano")
                    .summaryDate(FIXED_DATE)
                    .build();
            when(aiInsightRepository.findByUser(user)).thenReturn(Optional.of(dbInsight));
            when(summaryService.getDailySummary(user)).thenReturn(sampleMetrics);

            Optional<AiSummaryDTO> result = aiSummaryService.getCachedInsight(user);

            assertTrue(result.isPresent());
            assertEquals("DB summary", result.get().summary());
            verify(aiInsightCache).put(eq(userCacheKey), any(AiSummaryDTO.class));
        }

        @Test
        @DisplayName("should return empty when no insight in cache or database")
        void shouldReturnEmptyWhenNoInsightAnywhere() {
            when(aiInsightCache.getIfPresent(userCacheKey)).thenReturn(null);
            when(aiInsightRepository.findByUser(user)).thenReturn(Optional.empty());

            Optional<AiSummaryDTO> result = aiSummaryService.getCachedInsight(user);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("generateNewInsight")
    class GenerateNewInsightTests {

        @Test
        @DisplayName("should generate and store new insight in database and cache")
        void shouldGenerateAndStoreNewInsight() {
            when(summaryService.getDailySummary(user)).thenReturn(sampleMetrics);
            when(providerSelector.isProviderAvailable(AiProvider.AUTO)).thenReturn(true);
            when(providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.AUTO))
                    .thenReturn(AiProviderSelector.AiGenerationResult.success(
                            "New insight", "gpt-5-nano", AiProvider.OPENAI));
            when(aiInsightRepository.findByUser(user)).thenReturn(Optional.empty());

            AiSummaryDTO result = aiSummaryService.generateNewInsight(user, SummaryType.DEVELOPER, AiProvider.AUTO);

            assertEquals("New insight", result.summary());
            verify(aiInsightRepository).save(any(AiInsight.class));
            verify(aiInsightCache).put(eq(userCacheKey), any(AiSummaryDTO.class));
        }

        @Test
        @DisplayName("should replace existing insight in database")
        void shouldReplaceExistingInsight() {
            AiInsight existingInsight = AiInsight.builder()
                    .user(user)
                    .summaryType(SummaryType.EXECUTIVE)
                    .provider(AiProvider.OPENAI)
                    .summary("Old summary")
                    .aiGenerated(true)
                    .model("gpt-5-nano")
                    .summaryDate(FIXED_DATE)
                    .build();

            when(summaryService.getDailySummary(user)).thenReturn(sampleMetrics);
            when(providerSelector.isProviderAvailable(AiProvider.AUTO)).thenReturn(true);
            when(providerSelector.generateSummary(sampleMetrics, SummaryType.DEVELOPER, AiProvider.AUTO))
                    .thenReturn(AiProviderSelector.AiGenerationResult.success(
                            "New insight", "gpt-5-nano", AiProvider.OPENAI));
            when(aiInsightRepository.findByUser(user)).thenReturn(Optional.of(existingInsight));

            AiSummaryDTO result = aiSummaryService.generateNewInsight(user, SummaryType.DEVELOPER, AiProvider.AUTO);

            assertEquals("New insight", result.summary());
            verify(aiInsightRepository).save(existingInsight);
            assertEquals(SummaryType.DEVELOPER, existingInsight.getSummaryType());
        }
    }

    @Nested
    @DisplayName("invalidateInsightCache")
    class InvalidateInsightCacheTests {

        @Test
        @DisplayName("should invalidate both database and cache for user when insight exists")
        void shouldInvalidateBothDatabaseAndCacheWhenInsightExists() {
            UUID insightId = UUID.randomUUID();
            AiInsight existingInsight = AiInsight.builder()
                    .id(insightId)
                    .user(user)
                    .summaryType(SummaryType.DEVELOPER)
                    .provider(AiProvider.OPENAI)
                    .summary("Old summary")
                    .aiGenerated(true)
                    .model("gpt-5-nano")
                    .summaryDate(FIXED_DATE)
                    .build();
            when(aiInsightRepository.findByUser(user)).thenReturn(Optional.of(existingInsight));

            aiSummaryService.invalidateInsightCache(user);

            verify(aiInsightRepository).deleteByIdAndUser(insightId, user);
            verify(aiInsightCache).invalidate(userCacheKey);
        }

        @Test
        @DisplayName("should only invalidate cache when no insight exists in database")
        void shouldOnlyInvalidateCacheWhenNoInsightExists() {
            when(aiInsightRepository.findByUser(user)).thenReturn(Optional.empty());

            aiSummaryService.invalidateInsightCache(user);

            verify(aiInsightRepository, never()).deleteByIdAndUser(any(), any());
            verify(aiInsightCache).invalidate(userCacheKey);
        }
    }
}

