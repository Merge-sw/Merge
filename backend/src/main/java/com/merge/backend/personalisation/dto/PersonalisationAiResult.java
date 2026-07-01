package com.merge.backend.personalisation.dto;

import com.merge.backend.personalisation.domain.ScaffoldingLevel;

import java.util.List;
import java.util.Map;

/**
 * Structured analysis returned by the Gemini gateway (AI-01)
 * after processing a student's session data.
 */
public record PersonalisationAiResult(
        List<String> weakConcepts,
        List<String> strengthConcepts,
        ScaffoldingLevel scaffoldingLevel,
        Map<String, Object> codingStylePatterns
) {}
