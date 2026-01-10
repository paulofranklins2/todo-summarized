package org.duckdns.todosummarized.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.AiProvider;
import org.duckdns.todosummarized.domains.enums.SummaryType;
import org.duckdns.todosummarized.dto.AiSummaryDTO;
import org.duckdns.todosummarized.dto.DailySummaryDTO;
import org.duckdns.todosummarized.dto.SummaryTypeDTO;
import org.duckdns.todosummarized.ratelimit.RateLimit;
import org.duckdns.todosummarized.service.AiSummaryService;
import org.duckdns.todosummarized.service.AiProviderSelector;
import org.duckdns.todosummarized.service.SummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for daily summary operations.
 * All operations are scoped to the authenticated user.
 */
@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
@Tag(name = "Summary", description = "Daily summary and metrics API")
public class SummaryController {

    private final SummaryService summaryService;
    private final AiSummaryService aiSummaryService;

    /**
     * Get the daily summary with deterministic metrics for the authenticated user.
     */
    @Operation(
            summary = "Get daily summary",
            description = "Returns deterministic metrics summarizing the authenticated user's todos for the current day, " +
                    "including counts by status, priority, overdue items, and completion rate."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Summary retrieved successfully",
            content = @Content(schema = @Schema(implementation = DailySummaryDTO.class))
    )
    @GetMapping("/daily")
    public ResponseEntity<DailySummaryDTO> getDailySummary(@AuthenticationPrincipal User user) {
        DailySummaryDTO summary = summaryService.getDailySummary(user);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get AI-generated summary for the authenticated user.
     * Returns cached insight if available and matches the requested type.
     * Otherwise generates a new insight and caches it.
     * Falls back to metrics-only if AI is disabled or fails.
     */
    @Operation(
            summary = "Get AI-generated summary",
            description = "Returns the user's AI insight with cache-first strategy. " +
                    "If a cached insight exists and matches the requested type, returns it immediately (no AI call). " +
                    "Otherwise generates a new AI-powered summary and caches it. " +
                    "Supports multiple AI providers (OpenAI, Gemini) with automatic failover. " +
                    "Falls back to metrics-only if AI is disabled or encounters an error."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Summary retrieved successfully",
            content = @Content(schema = @Schema(implementation = AiSummaryDTO.class))
    )
    @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded"
    )
    @GetMapping("/ai")
    @RateLimit(key = "ai-summary")
    public ResponseEntity<AiSummaryDTO> getAiSummary(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Summary type/persona", example = "DEVELOPER")
            @RequestParam(defaultValue = "DEVELOPER") SummaryType type,
            @Parameter(description = "AI provider (AUTO, OPENAI, GEMINI)", example = "AUTO")
            @RequestParam(defaultValue = "AUTO") AiProvider provider
    ) {
        AiSummaryDTO summary = aiSummaryService.getAiSummary(user, type, provider);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get cached AI insight for the authenticated user.
     */
    @Operation(
            summary = "Get cached AI insight",
            description = "Returns the user's previously generated and cached AI insight. " +
                    "Use this when opening the AI insight modal to show existing insight immediately. " +
                    "Returns 204 No Content if no cached insight exists."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Cached insight found and returned",
            content = @Content(schema = @Schema(implementation = AiSummaryDTO.class))
    )
    @ApiResponse(
            responseCode = "204",
            description = "No cached insight available"
    )
    @GetMapping("/ai/cached")
    public ResponseEntity<AiSummaryDTO> getCachedAiInsight(@AuthenticationPrincipal User user) {
        return aiSummaryService.getCachedInsight(user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Get all available summary types with their descriptions.
     */
    @Operation(
            summary = "Get available summary types",
            description = "Returns all available summary type options with their display names and descriptions."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Summary types retrieved successfully",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SummaryTypeDTO.class)))
    )
    @GetMapping("/types")
    public ResponseEntity<List<SummaryTypeDTO>> getSummaryTypes() {
        return ResponseEntity.ok(aiSummaryService.getAvailableSummaryTypes());
    }

    /**
     * Check if AI summary feature is available.
     */
    @Operation(
            summary = "Check AI availability",
            description = "Returns whether the AI summary feature is currently enabled and available, " +
                    "including detailed information about each AI provider."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Status retrieved successfully"
    )
    @GetMapping("/ai/status")
    public ResponseEntity<Map<String, Object>> getAiStatus() {
        AiProviderSelector.ProviderInfo[] providerInfo = aiSummaryService.getProviderInfo();
        List<Map<String, Object>> providers = Arrays.stream(providerInfo)
                .map(info -> Map.<String, Object>of(
                        "provider", info.provider().name(),
                        "displayName", info.provider().getDisplayName(),
                        "available", info.available(),
                        "model", info.model() != null ? info.model() : ""
                ))
                .toList();

        return ResponseEntity.ok(Map.of(
                "available", aiSummaryService.isAiAvailable(),
                "providers", providers
        ));
    }
}

