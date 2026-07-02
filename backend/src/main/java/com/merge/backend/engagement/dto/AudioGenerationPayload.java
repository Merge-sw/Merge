package com.merge.backend.engagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for AUDIO_GENERATION jobs (AI-05).
 * Consumed by AudioGenerationHandler to produce a voiced explanation via Gemini.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AudioGenerationPayload {

    private Long studentId;
    private String sessionId;

    /** Concept the audio is generated for (current or next depending on audioType). */
    private Long conceptId;
    private String conceptName;

    /** REINFORCEMENT or PRIMER — drives the Gemini AudioWriter prompt. */
    private String audioType;

    /**
     * Target duration bounds in seconds.
     * REINFORCEMENT: 300–420 (5-7 min).
     * PRIMER: 0/0 — no hard constraint.
     */
    private int minDurationSeconds;
    private int maxDurationSeconds;
}
