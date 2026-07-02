package com.merge.backend.assessment.domain;

/** The five sequential gates a Build submission must pass. */
public enum BuildGate {
    /** Gate 1 — Functionality: code executes and all student tests pass (Judge0). */
    JUDGE0,
    /** Gate 2 — Architecture: architecture document evaluated against PRD requirements. */
    ARCHITECTURE,
    /** Gate 3 — Clean Code: naming, size, SOLID at the stage's clean-code level. */
    CLEAN_CODE,
    /** Gate 4 — Test Quality: student test suite evaluated for coverage and meaningfulness. */
    TEST_QUALITY,
    /** Gate 5 — Competency Signal: SFIA competencies demonstrated by the submission. */
    COMPETENCY_SIGNAL
}
