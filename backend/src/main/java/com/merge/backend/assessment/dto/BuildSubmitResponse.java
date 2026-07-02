package com.merge.backend.assessment.dto;

import com.merge.backend.assessment.domain.BuildSubmission;

import java.time.Instant;
import java.util.List;

public record BuildSubmitResponse(
        Long submissionId,
        Long buildId,
        int attemptNumber,
        String overallStatus,
        int pendingXp,
        Integer xpAwarded,
        Instant submittedAt,
        List<BuildGateResultDto> gates
) {
    public static BuildSubmitResponse from(BuildSubmission submission, List<BuildGateResultDto> gates) {
        return new BuildSubmitResponse(
                submission.getId(),
                submission.getBuild().getId(),
                submission.getAttemptNumber(),
                submission.getOverallStatus().name(),
                submission.getPendingXp(),
                submission.getXpAwarded(),
                submission.getSubmittedAt(),
                gates
        );
    }
}
