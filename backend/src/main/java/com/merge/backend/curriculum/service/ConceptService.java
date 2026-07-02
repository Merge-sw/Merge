package com.merge.backend.curriculum.service;

import com.merge.backend.curriculum.domain.Concept;
import com.merge.backend.curriculum.dto.ConceptSummary;
import com.merge.backend.curriculum.repository.ConceptRepository;
import com.merge.backend.curriculum.repository.ConceptUnlockRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConceptService {

    private static final List<String> STAGE_ORDER =
            List.of("SCOUT", "CADET", "ENGINEER", "ARCHITECT", "PRINCIPAL");

    private final ConceptRepository conceptRepository;
    private final ConceptUnlockRepository conceptUnlockRepository;
    private final ConceptUnlockService conceptUnlockService;
    private final StudentRepository studentRepository;

    public ConceptService(ConceptRepository conceptRepository,
                          ConceptUnlockRepository conceptUnlockRepository,
                          ConceptUnlockService conceptUnlockService,
                          StudentRepository studentRepository) {
        this.conceptRepository = conceptRepository;
        this.conceptUnlockRepository = conceptUnlockRepository;
        this.conceptUnlockService = conceptUnlockService;
        this.studentRepository = studentRepository;
    }

    /**
     * Returns concepts for a stage with per-concept unlock state.
     * Past stages (already graduated): all unlocked.
     * Current stage: per-concept unlock from concept_unlocks table; first concept
     *   is bootstrapped on first visit if no records exist yet.
     * Future stages: all locked.
     */
    @Transactional
    public List<ConceptSummary> getConceptsForStage(String stageType, String studentEmail) {
        String stage = stageType.toUpperCase();
        Student student = requireStudent(studentEmail);
        int studentRank = stageRank(student.getCurrentStage());
        int requestedRank = stageRank(stage);

        if (studentRank > requestedRank) {
            return conceptRepository.findByStageNameOrderBySequenceOrder(stage)
                    .stream().map(c -> ConceptSummary.from(c, true)).toList();
        }

        if (studentRank < requestedRank) {
            return conceptRepository.findByStageNameOrderBySequenceOrder(stage)
                    .stream().map(c -> ConceptSummary.from(c, false)).toList();
        }

        // Current stage — bootstrap concept #1 if no unlock records exist yet
        conceptUnlockService.ensureFirstConceptUnlocked(student, stage);

        return conceptRepository.findByStageNameOrderBySequenceOrder(stage)
                .stream()
                .map(c -> ConceptSummary.from(c,
                        conceptUnlockRepository.existsByStudentIdAndConceptId(
                                student.getId(), c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Concept getConcept(Long conceptId, String studentEmail) {
        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new ConceptNotFoundException("Concept not found: " + conceptId));

        Student student = requireStudent(studentEmail);
        int studentRank = stageRank(student.getCurrentStage());
        int conceptRank = stageRank(concept.getStageName());

        if (studentRank > conceptRank) {
            return concept;
        }
        if (studentRank < conceptRank) {
            throw new ConceptLockedException(
                    "Concept " + conceptId + " is not yet unlocked for this student");
        }

        // Same stage — check per-concept unlock record
        if (!conceptUnlockRepository.existsByStudentIdAndConceptId(
                student.getId(), conceptId)) {
            throw new ConceptLockedException(
                    "Concept " + conceptId + " is not yet unlocked for this student");
        }

        return concept;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Student requireStudent(String email) {
        return studentRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + email));
    }

    private int stageRank(String stage) {
        int rank = STAGE_ORDER.indexOf(stage.toUpperCase());
        return rank == -1 ? 0 : rank;
    }
}
