package com.merge.backend.assessment.exception;

import com.merge.backend.assessment.dto.BuildSubmitResponse;

public class DuplicateBuildSubmissionException extends RuntimeException {

    private final BuildSubmitResponse originalSubmission;

    public DuplicateBuildSubmissionException(BuildSubmitResponse originalSubmission) {
        super("Duplicate build submission idempotency key");
        this.originalSubmission = originalSubmission;
    }

    public BuildSubmitResponse getOriginalSubmission() {
        return originalSubmission;
    }
}
