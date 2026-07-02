package com.merge.backend.assessment.service;

import com.merge.backend.ai.gateway.GeminiGateway;
import com.merge.backend.assessment.domain.Build;
import com.merge.backend.assessment.dto.BuildPrdGenerationRequest;
import com.merge.backend.assessment.dto.BuildPrdResult;
import com.merge.backend.assessment.repository.BuildRepository;
import com.merge.backend.assessment.repository.BuildSubmissionRepository;
import com.merge.backend.assessment.repository.DrillCompletionRepository;
import com.merge.backend.curriculum.domain.Concept;
import com.merge.backend.curriculum.repository.ConceptRepository;
import com.merge.backend.personalisation.domain.PersonalisationProfile;
import com.merge.backend.personalisation.repository.PersonalisationProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BuildPrdGenerationService {

    private static final Logger log = LoggerFactory.getLogger(BuildPrdGenerationService.class);

    private final BuildRepository buildRepository;
    private final BuildSubmissionRepository buildSubmissionRepository;
    private final DrillCompletionRepository drillCompletionRepository;
    private final ConceptRepository conceptRepository;
    private final PersonalisationProfileRepository profileRepository;
    private final GeminiGateway geminiGateway;

    public BuildPrdGenerationService(BuildRepository buildRepository,
                                     BuildSubmissionRepository buildSubmissionRepository,
                                     DrillCompletionRepository drillCompletionRepository,
                                     ConceptRepository conceptRepository,
                                     PersonalisationProfileRepository profileRepository,
                                     GeminiGateway geminiGateway) {
        this.buildRepository = buildRepository;
        this.buildSubmissionRepository = buildSubmissionRepository;
        this.drillCompletionRepository = drillCompletionRepository;
        this.conceptRepository = conceptRepository;
        this.profileRepository = profileRepository;
        this.geminiGateway = geminiGateway;
    }

    /**
     * Gathers the student's complete Drill performance history for the stage,
     * calls AI-03 (BuildPrdWriter), and writes all five generated fields to the Build row.
     * Idempotent: returns immediately if prd is already populated.
     */
    @Transactional
    public void generate(Long buildId, Long studentId, String stageName) {
        Build build = buildRepository.findById(buildId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Build not found: " + buildId));

        if (build.getPrd() != null) {
            log.info("[BuildPrdGeneration] PRD already present for buildId={} — skipping", buildId);
            return;
        }

        PersonalisationProfile profile = profileRepository.findByStudentId(studentId).orElse(null);

        Map<String, Double> conceptPassRates = buildConceptPassRates(studentId, stageName);

        List<Integer> cleanCodeScores = buildSubmissionRepository
                .findCleanCodeScoresByStudentAndStage(studentId, stageName);

        BuildPrdGenerationRequest request = new BuildPrdGenerationRequest(
                studentId,
                stageName,
                profile != null ? nullSafe(profile.getWeakConcepts()) : List.of(),
                profile != null ? nullSafe(profile.getStrengthConcepts()) : List.of(),
                profile != null && profile.getScaffoldingLevel() != null
                        ? profile.getScaffoldingLevel().name() : null,
                profile != null && profile.getThinkingStyle() != null
                        ? profile.getThinkingStyle().name() : null,
                profile != null && profile.getLearningApproach() != null
                        ? profile.getLearningApproach().name() : null,
                conceptPassRates,
                profile != null ? nullSafeMap(profile.getHintUsagePattern()) : Map.of(),
                profile != null ? nullSafeObjMap(profile.getCodingStylePatterns()) : Map.of(),
                cleanCodeScores
        );

        log.info("[BuildPrdGeneration] Calling AI-03 for buildId={} studentId={} stage={}",
                buildId, studentId, stageName);

        BuildPrdResult result = geminiGateway.generateBuildPrd(request);

        build.setPrd(result.getPrd());
        build.setHiddenTestSuite(result.getHiddenTestSuite());
        build.setRequirements(result.getRequirements());
        build.setConstraints(result.getConstraints());
        build.setSfiaCompetencies(result.getSfiaCompetencies());
        buildRepository.save(build);

        log.info("[BuildPrdGeneration] PRD written for buildId={}", buildId);
    }

    /**
     * Queries DrillCompletion per concept and computes a 0.0–1.0 pass rate.
     * Rate = drillsPassedComprehension / 2 (two drills per concept: Drill 1 + Drill 2).
     * Concepts are ordered by sequenceOrder so Gemini receives them in curriculum order.
     */
    private Map<String, Double> buildConceptPassRates(Long studentId, String stageName) {
        List<Concept> concepts = conceptRepository.findByStageNameOrderBySequenceOrder(stageName);
        Map<String, Double> rates = new LinkedHashMap<>();
        for (Concept concept : concepts) {
            int passed = drillCompletionRepository
                    .countPassedComprehensionDrillsForConcept(studentId, concept.getId());
            rates.put(concept.getName(), Math.min(passed / 2.0, 1.0));
        }
        return rates;
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list != null ? list : List.of();
    }

    private static <K, V> Map<K, V> nullSafeMap(Map<K, V> map) {
        return map != null ? map : Map.of();
    }

    private static Map<String, Object> nullSafeObjMap(Map<String, Object> map) {
        return map != null ? map : Map.of();
    }
}
