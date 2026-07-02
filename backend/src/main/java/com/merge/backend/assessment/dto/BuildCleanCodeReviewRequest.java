package com.merge.backend.assessment.dto;

/** Gate 3 — AI-07: Clean code evaluation calibrated to the stage's clean-code level. */
public record BuildCleanCodeReviewRequest(
        String code,
        String stageName,
        /** NAMING_ONLY | NAMING_SIZE_REDUNDANCY | FULL_SOLID | HUMAN_REVIEW */
        String cleanCodeLevel
) {}
