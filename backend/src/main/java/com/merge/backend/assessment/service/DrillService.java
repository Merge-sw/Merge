package com.merge.backend.assessment.service;

import com.merge.backend.ai.gateway.GeminiGateway;
import com.merge.backend.assessment.domain.Drill;
import com.merge.backend.assessment.dto.DrillResponse;
import com.merge.backend.assessment.dto.GenerateDrillsRequest;
import com.merge.backend.assessment.dto.GeneratedDrill;
import com.merge.backend.assessment.repository.CodeReadingSubmissionRepository;
import com.merge.backend.assessment.repository.DrillCompletionRepository;
import com.merge.backend.assessment.repository.DrillRepository;
import com.merge.backend.curriculum.domain.Concept;
import com.merge.backend.curriculum.repository.ConceptRepository;
import com.merge.backend.curriculum.repository.ConceptUnlockRepository;
import com.merge.backend.curriculum.service.ConceptLockedException;
import com.merge.backend.curriculum.service.ConceptNotFoundException;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.personalisation.domain.PersonalisationProfile;
import com.merge.backend.personalisation.repository.PersonalisationProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class DrillService {

    private final DrillRepository drillRepository;
    private final DrillCompletionRepository drillCompletionRepository;
    private final CodeReadingSubmissionRepository codeReadingSubmissionRepository;
    private final ConceptRepository conceptRepository;
    private final ConceptUnlockRepository conceptUnlockRepository;
    private final StudentRepository studentRepository;
    private final PersonalisationProfileRepository profileRepository;
    private final GeminiGateway geminiGateway;

    public DrillService(DrillRepository drillRepository,
                        DrillCompletionRepository drillCompletionRepository,
                        CodeReadingSubmissionRepository codeReadingSubmissionRepository,
                        ConceptRepository conceptRepository,
                        ConceptUnlockRepository conceptUnlockRepository,
                        StudentRepository studentRepository,
                        PersonalisationProfileRepository profileRepository,
                        GeminiGateway geminiGateway) {
        this.drillRepository = drillRepository;
        this.drillCompletionRepository = drillCompletionRepository;
        this.codeReadingSubmissionRepository = codeReadingSubmissionRepository;
        this.conceptRepository = conceptRepository;
        this.conceptUnlockRepository = conceptUnlockRepository;
        this.studentRepository = studentRepository;
        this.profileRepository = profileRepository;
        this.geminiGateway = geminiGateway;
    }

    /**
     * Returns Drill 1 and Drill 2 for this student + concept.
     * Generates via AI-02 on first access and caches in the drills table.
     * Drill 2 locked: until Drill 1 comprehension check passes.
     * Drill 2 codeReadingCompleted: false until student submits code reading response.
     * 403 if concept not yet unlocked.
     */
    @Transactional
    public List<DrillResponse> getDrills(Long conceptId, String studentEmail) {
        Student student = studentRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentEmail));

        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new ConceptNotFoundException("Concept not found: " + conceptId));

        if (!conceptUnlockRepository.existsByStudentIdAndConceptId(student.getId(), conceptId)) {
            throw new ConceptLockedException("Concept " + conceptId + " is not yet unlocked for this student");
        }

        List<Drill> drills = drillRepository
                .findByStudentIdAndConceptIdOrderByDrillNumberAsc(student.getId(), conceptId);

        if (drills.isEmpty()) {
            drills = generate(student, concept);
        }

        Drill drill1 = drills.get(0);
        Drill drill2 = drills.get(1);

        boolean drill1Passed = drillCompletionRepository
                .existsByStudentIdAndDrillIdAndComprehensionPassedTrue(student.getId(), drill1.getId());

        boolean codeReadingDone = codeReadingSubmissionRepository
                .existsByStudentIdAndDrillId(student.getId(), drill2.getId());

        return List.of(
                DrillResponse.from(drill1, false, false),
                DrillResponse.from(drill2, !drill1Passed, codeReadingDone)
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<Drill> generate(Student student, Concept concept) {
        PersonalisationProfile profile = profileRepository
                .findByStudentId(student.getId()).orElse(null);

        GenerateDrillsRequest request = new GenerateDrillsRequest(
                concept.getName(),
                concept.getSfiaSkill(),
                concept.getFailureScenario(),
                profile != null && profile.getScaffoldingLevel() != null
                        ? profile.getScaffoldingLevel().name() : "MEDIUM",
                profile != null && profile.getThinkingStyle() != null
                        ? profile.getThinkingStyle().name() : "SYSTEMATIC",
                profile != null && profile.getLearningApproach() != null
                        ? profile.getLearningApproach().name() : "EXAMPLES_FIRST"
        );

        List<GeneratedDrill> generated = geminiGateway.generateDrills(request);

        Instant now = Instant.now();
        return generated.stream().map(g -> {
            Drill drill = new Drill();
            drill.setStudent(student);
            drill.setConcept(concept);
            drill.setDrillNumber(g.drillNumber());
            drill.setProblemStatement(g.problemStatement());
            drill.setStarterCode(g.starterCode());
            drill.setGeneratedAt(now);
            return drillRepository.save(drill);
        }).toList();
    }
}
