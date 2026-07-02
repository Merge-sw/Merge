package com.merge.backend.assessment.service;

import com.merge.backend.assessment.dto.DrillSubmitResponse;

public class DuplicateSubmissionException extends RuntimeException {

    private final DrillSubmitResponse originalSubmission;

    public DuplicateSubmissionException(DrillSubmitResponse originalSubmission) {
        super("Duplicate idempotency key");
        this.originalSubmission = originalSubmission;
    }

    public DrillSubmitResponse getOriginalSubmission() {
        return originalSubmission;
    }
}
