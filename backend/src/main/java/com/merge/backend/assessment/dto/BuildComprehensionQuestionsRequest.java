package com.merge.backend.assessment.dto;

/**
 * AI-10 request: generates comprehension questions grounded in the student's specific
 * Build artefacts — their actual code decisions, architecture choices, and test strategies.
 * Questions must be unanswerable without reading this exact implementation.
 *
 * minQuestions/maxQuestions instruct the model on the required question count range.
 * The caller trims to maxQuestions if the model returns more.
 */
public record BuildComprehensionQuestionsRequest(
        String code,
        String architectureDocument,
        String testSuite,
        String stageName,
        int minQuestions,
        int maxQuestions
) {}
