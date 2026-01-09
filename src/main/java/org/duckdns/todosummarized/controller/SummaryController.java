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
import org.duckdns.todosummarized.domains.enums.SummaryType;
import org.duckdns.todosummarized.dto.AiSummaryDTO;
import org.duckdns.todosummarized.dto.DailySummaryDTO;
import org.duckdns.todosummarized.dto.SummaryTypeDTO;
import org.duckdns.todosummarized.ratelimit.RateLimit;
import org.duckdns.todosummarized.service.AiSummaryService;
import org.duckdns.todosummarized.service.SummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded"
    )
    @GetMapping("/daily")
    @RateLimit(key = "daily-summary")
    public ResponseEntity<DailySummaryDTO> getDailySummary(@AuthenticationPrincipal User user) {
        DailySummaryDTO summary = summaryService.getDailySummary(user);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get AI-generated summary for the authenticated user.
     * Falls back to metrics-only if AI is disabled or fails.
     */
    @Operation(
            summary = "Get AI-generated summary",
            description = "Generates an AI-powered summary of the user's todos based on the selected summary type. " +
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
            @RequestParam(defaultValue = "DEVELOPER") SummaryType type
    ) {
        AiSummaryDTO summary = aiSummaryService.getAiSummary(user, type);
        return ResponseEntity.ok(summary);
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
            description = "Returns whether the AI summary feature is currently enabled and available."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Status retrieved successfully"
    )
    @GetMapping("/ai/status")
    public ResponseEntity<Map<String, Boolean>> getAiStatus() {
        return ResponseEntity.ok(Map.of("available", aiSummaryService.isAiAvailable()));
    }
}

