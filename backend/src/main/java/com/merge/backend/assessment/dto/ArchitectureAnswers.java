package com.merge.backend.assessment.dto;

/** FACT A — Architecture Reasoning fields. Both must be non-blank. */
public record ArchitectureAnswers(
        String dataLayout,
        String algorithmTradeoffs
) {}
