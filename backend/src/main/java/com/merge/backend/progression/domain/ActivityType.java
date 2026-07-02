package com.merge.backend.progression.domain;

public enum ActivityType {
    // ── Current canonical types (mapped in xp_caps) ──────────────────────────
    LEARNING_RESOURCE,
    /** First drill for a concept — easier, guided scaffold. */
    DRILL_1,
    /** Second drill for a concept — raised difficulty. */
    DRILL_2,
    BUILD,
    WEEKLY_CONSISTENCY,
    SEASON_RANKING,
    PEER_REVIEW,

    // ── Legacy types — kept for backward-compat with existing xp_entries rows ──
    /** @deprecated use DRILL_1 or DRILL_2 */
    DRILL_PASS,
    /** @deprecated use BUILD */
    BUILD_PASS,
    /** @deprecated use WEEKLY_CONSISTENCY */
    STREAK_BONUS
}
