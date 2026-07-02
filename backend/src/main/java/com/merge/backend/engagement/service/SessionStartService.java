package com.merge.backend.engagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merge.backend.assessment.repository.BuildRepository;
import com.merge.backend.assessment.repository.DrillCompletionRepository;
import com.merge.backend.curriculum.domain.Concept;
import com.merge.backend.curriculum.domain.Stage;
import com.merge.backend.curriculum.repository.ConceptRepository;
import com.merge.backend.curriculum.repository.ConceptUnlockRepository;
import com.merge.backend.curriculum.repository.StageRepository;
import com.merge.backend.engagement.domain.AudioType;
import com.merge.backend.engagement.domain.Session;
import com.merge.backend.engagement.domain.SessionMood;
import com.merge.backend.engagement.domain.SessionPlanStep;
import com.merge.backend.engagement.domain.SessionType;
import com.merge.backend.engagement.dto.AudioGenerationPayload;
import com.merge.backend.engagement.dto.SessionStartRequest;
import com.merge.backend.engagement.dto.SessionStartResponse;
import com.merge.backend.engagement.repository.SessionRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.infrastructure.queue.JobQueueService;
import com.merge.backend.infrastructure.queue.JobType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SessionStartService {

    private static final int REINFORCEMENT_MIN_SECONDS = 300;
    private static final int REINFORCEMENT_MAX_SECONDS = 420;

    private final StudentRepository studentRepository;
    private final ConceptUnlockRepository conceptUnlockRepository;
    private final BuildRepository buildRepository;
    private final SessionRepository sessionRepository;
    private final DrillCompletionRepository drillCompletionRepository;
    private final ConceptRepository conceptRepository;
    private final StageRepository stageRepository;
    private final JobQueueService jobQueueService;
    private final ObjectMapper objectMapper;

    public SessionStartService(StudentRepository studentRepository,
                               ConceptUnlockRepository conceptUnlockRepository,
                               BuildRepository buildRepository,
                               SessionRepository sessionRepository,
                               DrillCompletionRepository drillCompletionRepository,
                               ConceptRepository conceptRepository,
                               StageRepository stageRepository,
                               JobQueueService jobQueueService,
                               ObjectMapper objectMapper) {
        this.studentRepository = studentRepository;
        this.conceptUnlockRepository = conceptUnlockRepository;
        this.buildRepository = buildRepository;
        this.sessionRepository = sessionRepository;
        this.drillCompletionRepository = drillCompletionRepository;
        this.conceptRepository = conceptRepository;
        this.stageRepository = stageRepository;
        this.jobQueueService = jobQueueService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SessionStartResponse start(SessionStartRequest req, String studentEmail) {
        SessionMood mood = parseMood(req.mood());

        Student student = studentRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found"));

        Concept concept = conceptUnlockRepository
                .findTopByStudentIdAndConceptStageNameOrderByConceptSequenceOrderDesc(
                        student.getId(), student.getCurrentStage())
                .map(unlock -> unlock.getConcept())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "No concept unlocked for stage " + student.getCurrentStage()));

        SessionType sessionType = buildRepository.existsByStudentIdAndStageNameAndUnlockedTrue(
                student.getId(), student.getCurrentStage())
                ? SessionType.BUILD
                : SessionType.DRILL;

        Session session = new Session();
        session.setId(UUID.randomUUID().toString());
        session.setStudent(student);
        session.setConcept(concept);
        session.setMood(mood);
        session.setSessionType(sessionType);
        session.setStartedAt(Instant.now());
        sessionRepository.save(session);

        List<String> plan = buildPlan(mood, student, concept, session.getId());

        return new SessionStartResponse(session.getId(), concept.getId(), sessionType.name(), plan);
    }

    private List<String> buildPlan(SessionMood mood, Student student, Concept concept, String sessionId) {
        return switch (mood) {
            case FRESH -> buildFreshPlan(student);
            case OKAY  -> buildOkayPlan();
            case EXHAUSTED -> {
                routeExhausted(student, concept, sessionId);
                yield List.of();
            }
        };
    }

    private List<String> buildFreshPlan(Student student) {
        Stage stage = stageRepository.findById(student.getCurrentStage())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Stage not found: " + student.getCurrentStage()));

        List<String> plan = new ArrayList<>();
        plan.add(SessionPlanStep.FAILURE_SCENARIO.name());
        plan.add(SessionPlanStep.EXPLANATION.name());
        plan.add(SessionPlanStep.RESOURCES.name());
        if (stage.isHasSyntaxExercises()) {
            plan.add(SessionPlanStep.SYNTAX_EXERCISE.name());
        }
        plan.add(SessionPlanStep.DRILL_1.name());
        plan.add(SessionPlanStep.DRILL_2.name());
        return List.copyOf(plan);
    }

    private List<String> buildOkayPlan() {
        return List.of(
                SessionPlanStep.EXPLANATION.name(),
                SessionPlanStep.DRILL_1.name(),
                SessionPlanStep.DRILL_2.name()
        );
    }

    /** Determines REINFORCEMENT vs PRIMER and enqueues the AUDIO_GENERATION job. */
    private void routeExhausted(Student student, Concept concept, String sessionId) {
        int passedDrills = drillCompletionRepository
                .countPassedComprehensionDrillsForConcept(student.getId(), concept.getId());

        boolean atConceptBoundary = passedDrills >= 2;

        if (atConceptBoundary) {
            Concept nextConcept = conceptRepository
                    .findByStageNameAndSequenceOrder(concept.getStageName(), concept.getSequenceOrder() + 1)
                    .orElse(null);

            if (nextConcept != null) {
                enqueueAudio(student.getId(), sessionId, nextConcept, AudioType.PRIMER, 0, 0);
                return;
            }
        }

        enqueueAudio(student.getId(), sessionId, concept, AudioType.REINFORCEMENT,
                REINFORCEMENT_MIN_SECONDS, REINFORCEMENT_MAX_SECONDS);
    }

    private void enqueueAudio(Long studentId, String sessionId, Concept concept,
                               AudioType audioType, int minSeconds, int maxSeconds) {
        AudioGenerationPayload payload = new AudioGenerationPayload(
                studentId, sessionId,
                concept.getId(), concept.getName(),
                audioType.name(),
                minSeconds, maxSeconds);
        try {
            jobQueueService.enqueue(JobType.AUDIO_GENERATION, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialise AUDIO_GENERATION payload", e);
        }
    }

    private SessionMood parseMood(String raw) {
        if (raw == null) {
            throw new InvalidMoodException("null");
        }
        try {
            return SessionMood.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidMoodException(raw);
        }
    }
}
