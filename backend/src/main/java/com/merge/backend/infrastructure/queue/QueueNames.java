package com.merge.backend.infrastructure.queue;

import java.util.Map;

/**
 * Authoritative source of every Redis key used by the job queue system.
 * All producers and workers must reference these constants — never inline strings.
 */
public final class QueueNames {

    // ── Named job queues ──────────────────────────────────────────────────────
    public static final String GITHUB_COMMIT          = "merge:queue:github_commit";
    public static final String CLEAN_CODE_FEEDBACK    = "merge:queue:clean_code_feedback";
    public static final String PERSONALISATION_UPDATE = "merge:queue:personalisation_update";
    public static final String COMPETENCY_SIGNAL      = "merge:queue:competency_signal";
    public static final String BUILD_PRD_GENERATION   = "merge:queue:build_prd_generation";
    public static final String AUDIO_GENERATION       = "merge:queue:audio_generation";
    public static final String MOMENTUM_CALCULATION   = "merge:queue:momentum_calculation";
    public static final String DISENGAGEMENT_CHECK    = "merge:queue:disengagement_check";
    public static final String SEASON_LOCK            = "merge:queue:season_lock";
    public static final String SPOT_CHECK_PEER_REVIEW = "merge:queue:spot_check_peer_review";

    // ── System queues ─────────────────────────────────────────────────────────
    /** Sorted set — score = scheduled-at epoch millis. */
    public static final String RETRY = "merge:queue:retry";
    /** Dead letter queue — jobs that exhausted all retry attempts. */
    public static final String DLQ   = "merge:queue:dlq";

    /** Maps the kebab-case property key (job.worker.concurrency.*) to the Redis queue key. */
    public static final Map<String, String> BY_PROPERTY_KEY = Map.of(
            "github-commit",          GITHUB_COMMIT,
            "clean-code-feedback",    CLEAN_CODE_FEEDBACK,
            "personalisation-update", PERSONALISATION_UPDATE,
            "competency-signal",      COMPETENCY_SIGNAL,
            "build-prd-generation",   BUILD_PRD_GENERATION,
            "audio-generation",       AUDIO_GENERATION,
            "momentum-calculation",   MOMENTUM_CALCULATION,
            "disengagement-check",    DISENGAGEMENT_CHECK,
            "season-lock",            SEASON_LOCK,
            "spot-check-peer-review", SPOT_CHECK_PEER_REVIEW
    );

    private QueueNames() {}
}
