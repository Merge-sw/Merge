package com.merge.backend.ai.gateway;

import com.merge.backend.curriculum.dto.ConceptExplanationRequest;
import com.merge.backend.personalisation.dto.PersonalisationAiResult;
import com.merge.backend.personalisation.dto.SessionAnalysisPayload;

/**
 * AI-01: Gemini API gateway.
 * Implemented by the ai module; consumed by feature modules that need AI analysis.
 */
public interface GeminiGateway {

    /**
     * Sends session metrics to Gemini and returns a structured personalisation analysis.
     * Evaluates hint patterns, comprehension scores, pass/fail history, and question types
     * to classify weak/strength concepts, scaffolding level, and coding style signals.
     */
    PersonalisationAiResult analyseSessionForPersonalisation(SessionAnalysisPayload payload);

    /**
     * CurriculumWriter prompt: generates a 500-800 word personalised written explanation
     * of a concept, tailored to the student's scaffolding level, thinking style,
     * learning approach, and prior exposure.
     *
     * @return the generated explanation text (plain prose, no Markdown headers)
     */
    String generateConceptExplanation(ConceptExplanationRequest request);
}
