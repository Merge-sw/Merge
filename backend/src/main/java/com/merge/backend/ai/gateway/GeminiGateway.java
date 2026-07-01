package com.merge.backend.ai.gateway;

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
}
