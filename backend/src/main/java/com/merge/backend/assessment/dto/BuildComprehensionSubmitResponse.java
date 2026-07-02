package com.merge.backend.assessment.dto;

import java.util.List;

/**
 * Returned from POST /api/v1/build-comprehension/{id}/submit.
 *
 * comprehensionPassed — whether the comprehension check itself (gate 4) was answered correctly.
 *                       false means gate 4 failed; the submission is still PASSED at MINIMUM tier
 *                       because gates 1+2+3 had already passed.
 * xpAwarded          — actual XP credited after tier base × attempt decay.
 * tier               — MINIMUM / STANDARD / DISTINCTION (always present on this response).
 * gates              — all five gate results aggregated for display.
 */
public record BuildComprehensionSubmitResponse(
        boolean comprehensionPassed,
        int xpAwarded,
        String tier,
        List<BuildGateResultDto> gates
) {}
