package com.merge.backend.assessment.dto;

import java.util.List;

/** Gate 2 — AI-06: Architecture review against the personalised PRD. */
public record BuildArchitectureReviewRequest(
        String architectureDocument,
        String prd,
        List<String> requirements,
        List<String> constraints
) {}
