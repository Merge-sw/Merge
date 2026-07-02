package com.merge.backend.ai.gateway;

import com.merge.backend.assessment.dto.*;
import com.merge.backend.curriculum.dto.ConceptExplanationRequest;
import com.merge.backend.personalisation.domain.ScaffoldingLevel;
import com.merge.backend.personalisation.dto.PersonalisationAiResult;
import com.merge.backend.personalisation.dto.SessionAnalysisPayload;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class MockGeminiGateway implements GeminiGateway {

    @Override
    public PersonalisationAiResult analyseSessionForPersonalisation(SessionAnalysisPayload payload) {
        return new PersonalisationAiResult(
                Collections.emptyList(),
                Collections.emptyList(),
                ScaffoldingLevel.MEDIUM,
                Collections.emptyMap()
        );
    }

    @Override
    public String generateConceptExplanation(ConceptExplanationRequest request) {
        return "Mock concept explanation text.";
    }

    @Override
    public BuildPrdResult generateBuildPrd(BuildPrdGenerationRequest request) {
        BuildPrdResult res = new BuildPrdResult();
        res.setPrd("Mock Product Requirements Document.");
        res.setHiddenTestSuite("class MockTest {}");
        res.setRequirements(List.of("Req 1"));
        res.setConstraints(List.of("Constraint 1"));
        res.setSfiaCompetencies(List.of("DESN"));
        return res;
    }

    @Override
    public List<GeneratedDrill> generateDrills(GenerateDrillsRequest request) {
        GeneratedDrill d1 = new GeneratedDrill(1, "Mock Drill 1 statement.", "function f() {}");
        GeneratedDrill d2 = new GeneratedDrill(2, "Mock Drill 2 statement.", "function f() {}");
        return List.of(d1, d2);
    }

    @Override
    public List<ComprehensionQuestion> generateComprehensionQuestions(ComprehensionQuestionsRequest request) {
        return List.of(new ComprehensionQuestion("Mock comprehension question?"));
    }

    @Override
    public boolean scoreComprehensionAnswers(ComprehensionScoreRequest request) {
        return true;
    }

    @Override
    public boolean reviewBuildArchitecture(BuildArchitectureReviewRequest request) {
        return true;
    }

    @Override
    public CleanCodeReviewResult reviewBuildCleanCode(BuildCleanCodeReviewRequest request) {
        return new CleanCodeReviewResult(85, "Good naming style.");
    }

    @Override
    public boolean reviewBuildTestQuality(BuildTestQualityRequest request) {
        return true;
    }

    @Override
    public boolean evaluateBuildCompetencySignal(BuildCompetencySignalRequest request) {
        return true;
    }

    @Override
    public List<ComprehensionQuestion> generateBuildComprehensionQuestions(BuildComprehensionQuestionsRequest request) {
        return List.of(new ComprehensionQuestion("Mock build comprehension question?"));
    }

    @Override
    public boolean scoreBuildComprehensionAnswers(BuildComprehensionScoreRequest request) {
        return true;
    }

    @Override
    public String generateDisengagementReachOut(String studentName, String lastActiveConcept, List<String> recentStates) {
        return String.format("Hi %s. It looks like you've been inactive or blocked on %s recently. " +
                "Let's get back on track! Reach out if you need hints.", studentName, lastActiveConcept);
    }

    @Override
    public String generateAudioScript(String conceptName, String audioType, String learningApproach, List<String> weakConcepts) {
        return String.format("Welcome to this %s audiocast on %s. Tailored to your %s approach. Keep learning!",
                audioType, conceptName, learningApproach);
    }
}

