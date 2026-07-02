package com.merge.backend.assessment.dto;

/**
 * Payload sent to GeminiGateway.generateDrills (AI-02).
 * Carries concept context and student personalisation signals so Gemini can
 * produce two calibrated, distinct coding exercises for the same concept.
 */
public record GenerateDrillsRequest(
        String conceptName,
        String sfiaSkill,
        String failureScenario,
        String scaffoldingLevel,
        String thinkingStyle,
        String learningApproach
) {}
