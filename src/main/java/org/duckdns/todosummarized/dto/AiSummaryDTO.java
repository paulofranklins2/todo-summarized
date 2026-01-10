package org.duckdns.todosummarized.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.duckdns.todosummarized.domains.enums.SummaryType;

import java.time.LocalDate;

/**
 * Response DTO containing AI-generated summary with optional metrics fallback.
 */
@Builder
@Schema(description = "AI-generated summary response")
public record AiSummaryDTO(

        @Schema(description = "Date of the summary", example = "2026-01-09")
        LocalDate date,

        @Schema(description = "Summary type used for generation", example = "DEVELOPER")
        SummaryType summaryType,

        @Schema(description = "Display name of the summary type", example = "Software Engineer / Developer")
        String summaryTypeName,

        @Schema(description = "AI-generated summary text", example = "Today's progress: Completed 3 tasks...")
        String summary,

        @Schema(description = "Whether AI was used for generation", example = "true")
        boolean aiGenerated,

        @Schema(description = "Fallback reason if AI was not used", example = "AI is disabled")
        String fallbackReason,

        @Schema(description = "OpenAI model used (if AI generated)", example = "gpt-5-nano")
        String model,

        @Schema(description = "Metrics-only summary as fallback")
        DailySummaryDTO metrics
) {

    /**
     * Creates an AI-generated response.
     */
    public static AiSummaryDTO aiGenerated(LocalDate date, SummaryType type, String summary, String model, DailySummaryDTO metrics) {
        return AiSummaryDTO.builder()
                .date(date)
                .summaryType(type)
                .summaryTypeName(type.getDisplayName())
                .summary(summary)
                .aiGenerated(true)
                .fallbackReason(null)
                .model(model)
                .metrics(metrics)
                .build();
    }

    /**
     * Creates a fallback response when AI is unavailable.
     */
    public static AiSummaryDTO fallback(LocalDate date, SummaryType type, String reason, DailySummaryDTO metrics) {
        return AiSummaryDTO.builder()
                .date(date)
                .summaryType(type)
                .summaryTypeName(type.getDisplayName())
                .summary(null)
                .aiGenerated(false)
                .fallbackReason(reason)
                .model(null)
                .metrics(metrics)
                .build();
    }
}

