package com.merge.backend.assessment.service;

import com.merge.backend.assessment.domain.Drill;
import com.merge.backend.assessment.domain.DrillSubmission;
import com.merge.backend.assessment.domain.DrillSubmissionStatus;
import com.merge.backend.assessment.dto.DrillSubmitRequest;
import com.merge.backend.assessment.dto.DrillSubmitResponse;
import com.merge.backend.assessment.repository.CodeReadingSubmissionRepository;
import com.merge.backend.assessment.repository.DrillCompletionRepository;
import com.merge.backend.assessment.repository.DrillRepository;
import com.merge.backend.assessment.repository.DrillSubmissionRepository;
import com.merge.backend.curriculum.repository.ConceptUnlockRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DrillSubmissionService {

    private final DrillRepository drillRepository;
    private final DrillSubmissionRepository submissionRepository;
    private final DrillCompletionRepository drillCompletionRepository;
    private final CodeReadingSubmissionRepository codeReadingSubmissionRepository;
    private final ConceptUnlockRepository conceptUnlockRepository;
    private final StudentRepository studentRepository;

    public DrillSubmissionService(DrillRepository drillRepository,
                                  DrillSubmissionRepository submissionRepository,
                                  DrillCompletionRepository drillCompletionRepository,
                                  CodeReadingSubmissionRepository codeReadingSubmissionRepository,
                                  ConceptUnlockRepository conceptUnlockRepository,
                                  StudentRepository studentRepository) {
        this.drillRepository = drillRepository;
        this.submissionRepository = submissionRepository;
        this.drillCompletionRepository = drillCompletionRepository;
        this.codeReadingSubmissionRepository = codeReadingSubmissionRepository;
        this.conceptUnlockRepository = conceptUnlockRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * Validates and records a drill submission.
     *
     * Order of checks (stops at first failure):
     * 1. 400 — input validation (code, testSuite, architectureAnswers)
     * 2. 409 — duplicate idempotency key (returns original submission)
     * 3. 404 — drill not found
     * 4. 403 — student does not own this drill
     * 5. 403 — drill is locked (concept not unlocked, or Drill 2 prerequisites not met)
     *
     * Judge0 execution is async and handled downstream; this endpoint saves with PENDING status.
     */
    @Transactional
    public DrillSubmitResponse submit(Long drillId, DrillSubmitRequest req, String studentEmail) {
        validate(req);

        Optional<DrillSubmission> existing = submissionRepository
                .findByIdempotencyKey(req.idempotencyKey());
        if (existing.isPresent()) {
            throw new DuplicateSubmissionException(DrillSubmitResponse.from(existing.get()));
        }

        Drill drill = drillRepository.findById(drillId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Drill not found: " + drillId));

        Student student = studentRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentEmail));

        if (!drill.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This drill does not belong to you");
        }

        assertUnlocked(student, drill);

        DrillSubmission submission = new DrillSubmission();
        submission.setStudent(student);
        submission.setDrill(drill);
        submission.setCode(req.code());
        submission.setTestSuite(req.testSuite());
        submission.setArchitectureDataLayout(req.architectureAnswers().dataLayout());
        submission.setArchitectureAlgorithmTradeoffs(req.architectureAnswers().algorithmTradeoffs());
        submission.setIdempotencyKey(req.idempotencyKey());
        submission.setStatus(DrillSubmissionStatus.PENDING);
        submission.setSubmittedAt(Instant.now());

        return DrillSubmitResponse.from(submissionRepository.save(submission));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validate(DrillSubmitRequest req) {
        List<String> errors = new ArrayList<>();

        if (req.code() == null || req.code().isBlank()) {
            errors.add("code must not be empty");
        }
        if (req.testSuite() == null || req.testSuite().isBlank()) {
            errors.add("testSuite must not be empty — TDD is required");
        }
        if (req.architectureAnswers() == null) {
            errors.add("architectureAnswers must not be null");
        } else {
            if (req.architectureAnswers().dataLayout() == null
                    || req.architectureAnswers().dataLayout().isBlank()) {
                errors.add("architectureAnswers.dataLayout must not be empty");
            }
            if (req.architectureAnswers().algorithmTradeoffs() == null
                    || req.architectureAnswers().algorithmTradeoffs().isBlank()) {
                errors.add("architectureAnswers.algorithmTradeoffs must not be empty");
            }
        }
        if (req.idempotencyKey() == null || req.idempotencyKey().isBlank()) {
            errors.add("idempotencyKey must not be empty");
        }

        if (!errors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.join("; ", errors));
        }
    }

    private void assertUnlocked(Student student, Drill drill) {
        Long conceptId = drill.getConcept().getId();
        Long studentId = student.getId();

        if (!conceptUnlockRepository.existsByStudentIdAndConceptId(studentId, conceptId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Concept is not yet unlocked for this student");
        }

        if (drill.getDrillNumber() == 2) {
            boolean drill1Passed = drillRepository
                    .findByStudentIdAndConceptIdAndDrillNumber(studentId, conceptId, 1)
                    .map(d -> drillCompletionRepository
                            .existsByStudentIdAndDrillIdAndComprehensionPassedTrue(studentId, d.getId()))
                    .orElse(false);

            if (!drill1Passed) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Drill 1 comprehension check must pass before submitting Drill 2");
            }

            if (!codeReadingSubmissionRepository
                    .existsByStudentIdAndDrillId(studentId, drill.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Code reading must be submitted before accessing Drill 2");
            }
        }
    }
}
