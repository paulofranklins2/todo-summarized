package org.duckdns.todosummarized.domains.enums;

import lombok.Getter;

/**
 * Enumeration of supported AI providers for summary generation.
 */
@Getter
public enum AiProvider {

    /**
     * OpenAI (GPT models).
     */
    OPENAI("OpenAI", "Uses OpenAI GPT models for AI-powered summaries"),

    /**
     * Google Gemini.
     */
    GEMINI("Google Gemini", "Uses Google Gemini models for AI-powered summaries"),

    /**
     * Automatic selection - tries providers in order of priority.
     */
    AUTO("Auto", "Automatically selects the best available AI provider");

    private final String displayName;
    private final String description;

    AiProvider(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
