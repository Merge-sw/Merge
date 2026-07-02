package com.merge.backend.assessment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merge.backend.assessment.domain.Build;
import com.merge.backend.assessment.dto.BuildPrdPayload;
import com.merge.backend.assessment.repository.BuildRepository;
import com.merge.backend.assessment.repository.DrillCompletionRepository;
import com.merge.backend.curriculum.domain.Stage;
import com.merge.backend.curriculum.repository.ConceptRepository;
import com.merge.backend.curriculum.repository.StageRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.infrastructure.queue.JobQueueService;
import com.merge.backend.infrastructure.queue.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class BuildUnlockService {

    private static final Logger log = LoggerFactory.getLogger(BuildUnlockService.class);

    private final BuildRepository buildRepository;
    private final DrillCompletionRepository drillCompletionRepository;
    private final ConceptRepository conceptRepository;
    private final StageRepository stageRepository;
    private final JobQueueService jobQueueService;
    private final ObjectMapper objectMapper;

    public BuildUnlockService(BuildRepository buildRepository,
                               DrillCompletionRepository drillCompletionRepository,
                               ConceptRepository conceptRepository,
                               StageRepository stageRepository,
                               JobQueueService jobQueueService,
                               ObjectMapper objectMapper) {
        this.buildRepository = buildRepository;
        this.drillCompletionRepository = drillCompletionRepository;
        this.conceptRepository = conceptRepository;
        this.stageRepository = stageRepository;
        this.jobQueueService = jobQueueService;
        this.objectMapper = objectMapper;
    }

    /**
     * Simultaneously checks both unlock conditions:
     *   1. Every drill in the stage (Drill 1 + Drill 2 per concept) has a passing comprehension check
     *   2. student.totalXp >= stage.xpThreshold
     *
     * If and only if BOTH are true:
     *   - Creates a Build row with is_unlocked=true and records unlocked_at
     *   - Enqueues a BUILD_PRD_GENERATION job (AI-03) to generate the personalised PRD
     *
     * Idempotent: if the build is already unlocked this call is a no-op.
     *
     * Called after every comprehension check pass (AS-05) because that's the only
     * event that can satisfy condition 1 — XP was also just awarded in the same request.
     */
    @Transactional
    public void checkAndUnlock(Student student, String stageName) {
        // Idempotent guard
        boolean alreadyUnlocked = buildRepository
                .findByStudentIdAndStageName(student.getId(), stageName)
                .map(Build::isUnlocked)
                .orElse(false);
        if (alreadyUnlocked) return;

        Stage stage = stageRepository.findById(stageName)
                .orElse(null);
        if (stage == null) {
            log.warn("Stage '{}' not found during build unlock check for student {}", stageName, student.getId());
            return;
        }

        // Condition 2: XP threshold
        if (student.getTotalXp() < stage.getXpThreshold()) return;

        // Condition 1: all drills for this stage have passing comprehension checks
        long conceptCount = conceptRepository.countByStageName(stageName);
        if (conceptCount == 0) return;

        long requiredDrillCount = conceptCount * 2; // Drill 1 + Drill 2 per concept
        long passedDrillCount = drillCompletionRepository
                .countPassedDrillsForStage(student.getId(), stageName);

        if (passedDrillCount < requiredDrillCount) return;

        // Both conditions met — unlock and queue PRD generation
        Build build = new Build();
        build.setStudent(student);
        build.setStageName(stageName);
        build.setUnlocked(true);
        build.setUnlockedAt(Instant.now());
        build.setCreatedAt(Instant.now());
        Build saved = buildRepository.save(build);

        enqueuePrdGeneration(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void enqueuePrdGeneration(Build build) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new BuildPrdPayload(build.getId(), build.getStudent().getId(), build.getStageName()));
            jobQueueService.enqueue(JobType.BUILD_PRD_GENERATION, payload);
        } catch (JsonProcessingException e) {
            // Build row is committed; if enqueueing fails, the build stays unlocked with prd=null.
            // The GET endpoint returns 202, and operators can re-enqueue from the DLQ.
            log.error("Failed to enqueue BUILD_PRD_GENERATION for buildId={}: {}",
                    build.getId(), e.getMessage());
        }
    }
}
