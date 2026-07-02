package com.merge.backend.engagement.domain;

/**
 * Ordered steps that make up a session plan.
 * FRESH: FAILURE_SCENARIO → EXPLANATION → RESOURCES → [SYNTAX_EXERCISE] → DRILL_1 → DRILL_2
 * OKAY:  EXPLANATION → DRILL_1 → DRILL_2
 * EXHAUSTED: no plan — async audio queued instead
 */
public enum SessionPlanStep {
    FAILURE_SCENARIO,
    EXPLANATION,
    RESOURCES,
    /** Only included when the student's stage has syntax exercises (CADET in MVP). */
    SYNTAX_EXERCISE,
    DRILL_1,
    DRILL_2
}
