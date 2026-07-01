package com.merge.backend.ai.context;

import java.util.List;
import java.util.Map;

/**
 * A single row returned by the pgvector similarity query on personalisation_profiles.
 * Carries only the columns needed to build a Gemini prompt context block.
 */
public record PersonalisationContextEntry(
        Long profileId,
        Long studentId,
        List<String> weakConcepts,
        Map<String, Integer> hintUsagePattern,
        Map<String, Object> codingStylePatterns
) {}
