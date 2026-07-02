package com.merge.backend.engagement.dto;

import java.util.List;

/**
 * plan — ordered list of SessionPlanStep names the client should present.
 * Empty when mood is EXHAUSTED (async audio is queued instead).
 */
public record SessionStartResponse(
        String sessionId,
        Long conceptId,
        String sessionType,
        List<String> plan
) {}
