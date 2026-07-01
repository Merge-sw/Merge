package com.merge.backend.personalisation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Serialised as payloadJson on a PERSONALISATION_UPDATE job.
 * Produced by EN-02 at session end and consumed by PersonalisationUpdateHandler.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionAnalysisPayload {

    private Long studentId;
    private String sessionId;

    /** Total session wall-clock duration in milliseconds. */
    private long sessionDurationMs;

    /** Number of hints the student requested across the whole session. */
    private int hintUsageCount;

    /** Hints broken down by concept: concept name → count. */
    private Map<String, Integer> hintUsageByConcept;

    /** Comprehension check score per concept: concept name → score (0.0–1.0). */
    private Map<String, Double> comprehensionScoresByConcept;

    /** Drill pass/fail by drill ID: drillId → true (pass) / false (fail). */
    private Map<String, Boolean> passFailPatterns;

    /** Mean time spent per drill across the session, in milliseconds. */
    private long avgTimePerDrillMs;

    /** Question types the student failed at least once during the session. */
    private List<String> questionTypesFailed;
}
