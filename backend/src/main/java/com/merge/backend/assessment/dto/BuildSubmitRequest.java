package com.merge.backend.assessment.dto;

public record BuildSubmitRequest(
        String code,
        String testSuite,
        String architectureDocument,
        String idempotencyKey
) {}
