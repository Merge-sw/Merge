package com.merge.backend.personalisation.service;

import com.merge.backend.ai.embedding.EmbeddingUpdateService;
import com.merge.backend.ai.gateway.GeminiGateway;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.personalisation.domain.PersonalisationProfile;
import com.merge.backend.personalisation.dto.PersonalisationAiResult;
import com.merge.backend.personalisation.dto.SessionAnalysisPayload;
import com.merge.backend.personalisation.repository.PersonalisationProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class PersonalisationUpdateService {

    private static final Logger log = LoggerFactory.getLogger(PersonalisationUpdateService.class);

    /** EMA smoothing factor — 30% weight on the new session, 70% on history. */
    private static final double EMA_ALPHA = 0.3;

    private final PersonalisationProfileRepository profileRepository;
    private final StudentRepository studentRepository;
    private final GeminiGateway geminiGateway;
    private final EmbeddingUpdateService embeddingUpdateService;

    public PersonalisationUpdateService(PersonalisationProfileRepository profileRepository,
                                        StudentRepository studentRepository,
                                        GeminiGateway geminiGateway,
                                        EmbeddingUpdateService embeddingUpdateService) {
        this.profileRepository = profileRepository;
        this.studentRepository = studentRepository;
        this.geminiGateway = geminiGateway;
        this.embeddingUpdateService = embeddingUpdateService;
    }

    public void processSessionUpdate(SessionAnalysisPayload payload) {
        log.debug("[Personalisation] Processing session={} student={}",
                payload.getSessionId(), payload.getStudentId());

        PersonalisationProfile profile = profileRepository
                .findByStudentId(payload.getStudentId())
                .orElseGet(() -> createBlankProfile(payload.getStudentId()));

        PersonalisationAiResult aiResult = geminiGateway.analyseSessionForPersonalisation(payload);

        profile.setWeakConcepts(aiResult.weakConcepts());
        profile.setStrengthConcepts(aiResult.strengthConcepts());
        profile.setScaffoldingLevel(aiResult.scaffoldingLevel());
        profile.setAvgSessionDuration(computeAvgDuration(profile.getAvgSessionDuration(), payload.getSessionDurationMs()));
        profile.setHintUsagePattern(mergeHintUsage(profile.getHintUsagePattern(), payload.getHintUsageByConcept()));
        profile.setCodingStylePatterns(aiResult.codingStylePatterns());
        profile.setUpdatedAt(Instant.now());

        profileRepository.save(profile);

        log.debug("[Personalisation] Profile updated for student={} scaffolding={}",
                payload.getStudentId(), aiResult.scaffoldingLevel());

        embeddingUpdateService.triggerPersonalisationEmbeddingUpdate(payload.getStudentId());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private PersonalisationProfile createBlankProfile(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        PersonalisationProfile profile = new PersonalisationProfile();
        profile.setStudent(student);
        return profile;
    }

    private long computeAvgDuration(Long existing, long newDurationMs) {
        if (existing == null || existing == 0) return newDurationMs;
        return Math.round((1 - EMA_ALPHA) * existing + EMA_ALPHA * newDurationMs);
    }

    private Map<String, Integer> mergeHintUsage(Map<String, Integer> existing,
                                                 Map<String, Integer> sessionHints) {
        Map<String, Integer> merged = existing != null ? new HashMap<>(existing) : new HashMap<>();
        if (sessionHints != null) {
            sessionHints.forEach((concept, count) ->
                    merged.merge(concept, count, Integer::sum));
        }
        return merged;
    }
}
