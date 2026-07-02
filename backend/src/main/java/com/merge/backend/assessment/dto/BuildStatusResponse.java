package com.merge.backend.assessment.dto;

import com.merge.backend.assessment.domain.Build;

import java.time.Instant;

public record BuildStatusResponse(
        Long buildId,
        String stageName,
        String status,
        Instant unlockedAt,
        String prd
) {
    public static BuildStatusResponse from(Build build) {
        String status = build.getPrd() != null ? "READY" : "GENERATING";
        return new BuildStatusResponse(
                build.getId(),
                build.getStageName(),
                status,
                build.getUnlockedAt(),
                build.getPrd()
        );
    }
}
