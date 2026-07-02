package com.merge.backend.curriculum.service;

import com.merge.backend.curriculum.domain.Concept;
import com.merge.backend.curriculum.dto.ConceptSummary;
import com.merge.backend.curriculum.repository.ConceptRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConceptService {

    private static final List<String> STAGE_ORDER =
            List.of("SCOUT", "CADET", "ENGINEER", "ARCHITECT", "PRINCIPAL");

    private final ConceptRepository conceptRepository;
    private final StudentRepository studentRepository;

    public ConceptService(ConceptRepository conceptRepository,
                          StudentRepository studentRepository) {
        this.conceptRepository = conceptRepository;
        this.studentRepository = studentRepository;
    }

    public List<ConceptSummary> getConceptsForStage(String stageType, String studentEmail) {
        String stage = stageType.toUpperCase();
        Student student = requireStudent(studentEmail);
        boolean stageUnlocked = stageRank(student.getCurrentStage()) >= stageRank(stage);

        return conceptRepository.findByStageNameOrderBySequenceOrder(stage)
                .stream()
                .map(c -> ConceptSummary.from(c, stageUnlocked))
                .toList();
    }

    public Concept getConcept(Long conceptId, String studentEmail) {
        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new ConceptNotFoundException("Concept not found: " + conceptId));

        Student student = requireStudent(studentEmail);
        if (stageRank(student.getCurrentStage()) < stageRank(concept.getStageName())) {
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
