package com.merge.backend.assessment.dto;

public record DrillSubmitRequest(
        String code,
        String testSuite,
        ArchitectureAnswers architectureAnswers,
        String idempotencyKey
) {}
