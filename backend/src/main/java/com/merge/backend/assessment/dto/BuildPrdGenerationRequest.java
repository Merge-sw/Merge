package com.merge.backend.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Input to AI-03 (BuildPrdWriter). Carries the student's full Drill performance history
 * so Gemini can target the PRD at their specific gaps and patterns.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildPrdGenerationRequest {

    private Long studentId;
    private String stageName;

    // ── Personalisation profile ───────────────────────────────────────────────

    /** Concepts where the student's drill pass rate was < 100%. */
    private List<String> weakConcepts;

    /** Concepts the student mastered on the first attempt. */
    private List<String> strengthConcepts;

    /** NONE | LIGHT | MODERATE | HEAVY */
    private String scaffoldingLevel;

    /** ANALYTICAL | INTUITIVE | PRACTICAL */
    private String thinkingStyle;

    /** CONCEPTUAL_FIRST | EXAMPLE_FIRST | PROBLEM_FIRST */
    private String learningApproach;

    // ── Drill performance ─────────────────────────────────────────────────────

    /**
     * Comprehension pass rate per concept across the stage: concept name → 0.0–1.0.
     * 1.0 = both Drill 1 and Drill 2 passed first try; 0.5 = one of two passed.
     */
    private Map<String, Double> conceptPassRates;

    /** Cumulative hint requests per concept: concept name → count. Null entries treated as 0. */
    private Map<String, Integer> hintUsagePattern;

    /** AI-derived coding style signals accumulated from prior session analyses. */
    private Map<String, Object> codingStylePatterns;

    // ── Prior build feedback (empty for first unlock) ─────────────────────────

    /**
     * Gate 3 (AI-07 clean-code) scores from all previous build submission attempts.
     * Empty list on first unlock. Allows Gemini to detect recurring clean-code weaknesses.
     */
    private List<Integer> previousCleanCodeScores;
}
