package com.merge.backend.ai.gateway;

import com.merge.backend.assessment.dto.BuildArchitectureReviewRequest;
import com.merge.backend.assessment.dto.BuildCleanCodeReviewRequest;
import com.merge.backend.assessment.dto.BuildCompetencySignalRequest;
import com.merge.backend.assessment.dto.BuildTestQualityRequest;
import com.merge.backend.assessment.dto.CleanCodeReviewResult;
import com.merge.backend.assessment.dto.ComprehensionQuestion;
import com.merge.backend.assessment.dto.ComprehensionQuestionsRequest;
import com.merge.backend.assessment.dto.ComprehensionScoreRequest;
import com.merge.backend.assessment.dto.GenerateDrillsRequest;
import com.merge.backend.assessment.dto.GeneratedDrill;
import com.merge.backend.curriculum.dto.ConceptExplanationRequest;
import com.merge.backend.personalisation.dto.PersonalisationAiResult;
import com.merge.backend.personalisation.dto.SessionAnalysisPayload;

import java.util.List;

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

    /**
     * AI-02 — DrillWriter prompt: generates Drill 1 and Drill 2 for a concept,
     * calibrated to the student's personalisation profile.
     * Drill 1 is simpler (guided scaffold); Drill 2 raises the difficulty.
     * Always returns exactly 2 elements ordered by drillNumber.
     */
    List<GeneratedDrill> generateDrills(GenerateDrillsRequest request);

    /**
     * AI-04 — ComprehensionWriter prompt: generates questions grounded in the student's
     * specific implementation — their actual variable names, function choices, and
     * architectural decisions. Questions must be unanswerable without reading this exact code.
     *
     * Always called with fresh context so questions differ across submission attempts.
     * Returns a list of questions; caller computes serverDeadline as
     * triggeredAt + (result.size() × 10 seconds).
     */
    List<ComprehensionQuestion> generateComprehensionQuestions(ComprehensionQuestionsRequest request);

    /**
     * AI-05 — ComprehensionScorer prompt: evaluates the student's answers against
     * their specific code implementation. Verifies that answers reference concrete details
     * of the submitted code (variable names, chosen algorithms, data structures).
     * Generic answers that could apply to any implementation are scored as failed.
     *
     * @return true if the student demonstrated genuine understanding; false otherwise
     */
    boolean scoreComprehensionAnswers(ComprehensionScoreRequest request);

    /**
     * AI-06 — Build Gate 2: reviews the student's architecture document against the
     * personalised PRD requirements and constraints. Checks that every requirement
     * is addressed and constraints are respected in the documented design.
     *
     * @return true if the architecture document sufficiently addresses the PRD; false otherwise
     */
    boolean reviewBuildArchitecture(BuildArchitectureReviewRequest request);

    /**
     * AI-07 — CleanCodeReviewer prompt: scores the student's code against the
     * stage-appropriate rubric (NAMING_ONLY → NAMING_SIZE_REDUNDANCY → FULL_SOLID).
     * Only violations relevant to the stage level are penalised.
     * Returns a 0–100 score and dimension-specific feedback.
     * Caller compares score against Stage.cleanCodeMinScore to determine pass/fail.
     */
    CleanCodeReviewResult reviewBuildCleanCode(BuildCleanCodeReviewRequest request);

    /**
     * AI-08 — Build Gate 4: evaluates the student's test suite for coverage,
     * edge-case handling, and alignment with the PRD requirements.
     * Tests that trivially pass or duplicate production code without asserting behaviour are penalised.
     *
     * @return true if the test suite demonstrates meaningful quality; false otherwise
     */
    boolean reviewBuildTestQuality(BuildTestQualityRequest request);

    /**
     * AI-09 — Build Gate 5: assesses whether the submission demonstrates the SFIA competencies
     * this build was designed to target. Evaluates code, tests, and architecture together.
     *
     * @return true if competency signals are present; false otherwise
     */
    boolean evaluateBuildCompetencySignal(BuildCompetencySignalRequest request);
}
