package com.merge.backend.assessment.service;

import com.merge.backend.assessment.domain.Drill;
import com.merge.backend.assessment.domain.DrillSubmission;
import com.merge.backend.assessment.domain.DrillSubmissionStatus;
import com.merge.backend.assessment.dto.DrillSubmitRequest;
import com.merge.backend.assessment.dto.DrillSubmitResponse;
import com.merge.backend.assessment.judge0.Judge0ExecutionService;
import com.merge.backend.assessment.judge0.Judge0Outcome;
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
    private final Judge0ExecutionService judge0ExecutionService;
    private final ComprehensionCheckService comprehensionCheckService;

    public DrillSubmissionService(DrillRepository drillRepository,
                                  DrillSubmissionRepository submissionRepository,
                                  DrillCompletionRepository drillCompletionRepository,
                                  CodeReadingSubmissionRepository codeReadingSubmissionRepository,
                                  ConceptUnlockRepository conceptUnlockRepository,
                                  StudentRepository studentRepository,
                                  Judge0ExecutionService judge0ExecutionService,
                                  ComprehensionCheckService comprehensionCheckService) {
        this.drillRepository = drillRepository;
        this.submissionRepository = submissionRepository;
        this.drillCompletionRepository = drillCompletionRepository;
        this.codeReadingSubmissionRepository = codeReadingSubmissionRepository;
        this.conceptUnlockRepository = conceptUnlockRepository;
        this.studentRepository = studentRepository;
        this.judge0ExecutionService = judge0ExecutionService;
        this.comprehensionCheckService = comprehensionCheckService;
    }

    /**
     * Validates, records, and executes a drill submission through Judge0.
     *
     * Order of checks (stops at first failure):
     * 1. 400 — input validation (code, testSuite, architectureAnswers, idempotencyKey)
     * 2. 409 — duplicate idempotency key (returns original submission)
     * 3. 404 — drill not found
     * 4. 403 — student does not own this drill
     * 5. 403 — drill is locked (concept not unlocked, or Drill 2 prerequisites not met)
     *
     * Judge0 status mapping after execution:
     *   3 = accepted → 200, testsPassed: true  (comprehension check follows)
     *   4 = wrong answer → 200, testsPassed: false
     *   5 = timeout → 408
     *   6 = compilation error → 400 with stderr
     *   11+ = runtime error → 400
     *
     * noRollbackFor ensures the submission row (with updated status + stderr) is always committed,
     * even when we throw a ResponseStatusException for error cases.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
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

        int previousAttempts = submissionRepository.countByStudentIdAndDrillId(student.getId(), drillId);
        int attemptNumber = previousAttempts + 1;

        DrillSubmission submission = new DrillSubmission();
        submission.setStudent(student);
        submission.setDrill(drill);
        submission.setCode(req.code());
        submission.setTestSuite(req.testSuite());
        submission.setArchitectureDataLayout(req.architectureAnswers().dataLayout());
        submission.setArchitectureAlgorithmTradeoffs(req.architectureAnswers().algorithmTradeoffs());
        submission.setAttemptNumber(attemptNumber);
        submission.setIdempotencyKey(req.idempotencyKey());
        submission.setStatus(DrillSubmissionStatus.PENDING);
        submission.setSubmittedAt(Instant.now());

        // Flush to DB before calling Judge0 so the row exists if the process is interrupted
        submission = submissionRepository.saveAndFlush(submission);

        Judge0Outcome outcome = judge0ExecutionService.execute(req.code(), req.testSuite());

        // Update submission with Judge0 result — committed even if we throw below (noRollbackFor)
        submission.setStderr(outcome.stderr());
        switch (outcome.statusId()) {
            case 3 -> submission.setStatus(DrillSubmissionStatus.JUDGE0_PASS);
            case 4 -> submission.setStatus(DrillSubmissionStatus.JUDGE0_FAIL);
            default -> submission.setStatus(DrillSubmissionStatus.JUDGE0_FAIL);
        }
        submission = submissionRepository.save(submission);

        mapOutcomeToHttpException(outcome);

        // Status 3 (Accepted) — trigger comprehension check immediately using the student's
        // specific code so questions are grounded in their actual variable names and decisions.
        if (outcome.testsPassed()) {
            comprehensionCheckService.triggerFor(submission);
        }

        return DrillSubmitResponse.from(submission, outcome.testsPassed());
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

    /**
     * Translates a Judge0 terminal status into the appropriate HTTP exception.
     * Called after status has already been persisted to the DB.
     * Status 3 (accepted) and 4 (wrong answer) are both 200 — no exception thrown.
     */
    private void mapOutcomeToHttpException(Judge0Outcome outcome) {
        switch (outcome.statusId()) {
            case 3, 4 -> { /* 200 — fall through */ }
            case 5 -> throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT,
                    "Execution exceeded the 2-second time limit");
            case 6 -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Compilation error: " + outcome.stderr());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Runtime error — check your code for unhandled exceptions");
        }
    }
}
