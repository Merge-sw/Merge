package com.merge.backend.assessment.dto;

/**
 * AI-07 response: numeric clean-code score plus dimension-specific feedback.
 * Score is 0–100; the pass threshold is read from stages.clean_code_min_score.
 */
public record CleanCodeReviewResult(
        /** 0–100 score against the stage-appropriate rubric. */
        int score,
        /** Human-readable feedback identifying specific violations. Null when score is perfect. */
        String feedback
) {}
