package com.merge.backend.curriculum.service;

import com.merge.backend.assessment.repository.DrillCompletionRepository;
import com.merge.backend.curriculum.domain.Concept;
import com.merge.backend.curriculum.domain.ConceptUnlock;
import com.merge.backend.curriculum.repository.ConceptRepository;
import com.merge.backend.curriculum.repository.ConceptUnlockRepository;
import com.merge.backend.identity.domain.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class ConceptUnlockService {

    private static final int DRILLS_REQUIRED = 2;

    private final DrillCompletionRepository drillCompletionRepository;
    private final ConceptRepository conceptRepository;
    private final ConceptUnlockRepository conceptUnlockRepository;

    public ConceptUnlockService(DrillCompletionRepository drillCompletionRepository,
                                ConceptRepository conceptRepository,
                                ConceptUnlockRepository conceptUnlockRepository) {
        this.drillCompletionRepository = drillCompletionRepository;
        this.conceptRepository = conceptRepository;
        this.conceptUnlockRepository = conceptUnlockRepository;
    }

    /**
     * Called after a DrillCompletion with comprehensionPassed=true is saved.
     * If both Drill 1 AND Drill 2 for the concept now have comprehension passed,
     * the next concept in sequence (same stage, sequenceOrder + 1) is unlocked.
     */
    @Transactional
    public void triggerUnlockIfEligible(Student student, Concept concept) {
        int passed = drillCompletionRepository
                .countPassedComprehensionDrillsForConcept(student.getId(), concept.getId());

        if (passed < DRILLS_REQUIRED) {
            return;
        }

        Optional<Concept> next = conceptRepository.findByStageNameAndSequenceOrder(
                concept.getStageName(), concept.getSequenceOrder() + 1);

        next.ifPresent(nextConcept -> unlock(student, nextConcept));
    }

    /**
     * Ensures the first concept of a stage is unlocked for the student.
     * Called as a bootstrap when no unlock records exist yet for a stage the
     * student has already reached (e.g. on first visit to the CADET concept list).
     */
    @Transactional
    public void ensureFirstConceptUnlocked(Student student, String stageName) {
        if (conceptUnlockRepository.existsByStudentIdAndConceptStageName(
                student.getId(), stageName)) {
            return;
        }
        conceptRepository.findFirstByStageNameOrderBySequenceOrderAsc(stageName)
                .ifPresent(first -> unlock(student, first));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void unlock(Student student, Concept concept) {
        if (conceptUnlockRepository.existsByStudentIdAndConceptId(
                student.getId(), concept.getId())) {
            return;
        }
        ConceptUnlock unlock = new ConceptUnlock();
        unlock.setStudent(student);
        unlock.setConcept(concept);
        unlock.setUnlockedAt(Instant.now());
        conceptUnlockRepository.save(unlock);
    }
}
