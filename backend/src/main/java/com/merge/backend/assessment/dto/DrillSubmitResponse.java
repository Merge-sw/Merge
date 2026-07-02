package com.merge.backend.assessment.dto;

import com.merge.backend.assessment.domain.DrillSubmission;

import java.time.Instant;

public record DrillSubmitResponse(
        Long submissionId,
        Long drillId,
        String status,
        Instant submittedAt
) {
    public static DrillSubmitResponse from(DrillSubmission s) {
        return new DrillSubmitResponse(
                s.getId(),
                s.getDrill().getId(),
                s.getStatus().name(),
                s.getSubmittedAt()
        );
    }
}
