package com.merge.backend.assessment.dto;

/** Payload enqueued with BUILD_PRD_GENERATION so the worker knows which build to populate. */
public record BuildPrdPayload(Long buildId, Long studentId, String stageName) {}
