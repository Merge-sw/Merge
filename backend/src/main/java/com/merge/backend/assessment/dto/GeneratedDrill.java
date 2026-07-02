package com.merge.backend.assessment.dto;

/** One AI-generated drill returned by GeminiGateway.generateDrills. */
public record GeneratedDrill(
        int drillNumber,
        String problemStatement,
        String starterCode
) {}
