package com.merge.backend.assessment.service;

import com.merge.backend.ai.gateway.GeminiGateway;
import com.merge.backend.assessment.domain.ComprehensionCheck;
import com.merge.backend.assessment.domain.ComprehensionCheckStatus;
import com.merge.backend.assessment.domain.DrillCompletion;
import com.merge.backend.assessment.dto.ComprehensionScoreRequest;
import com.merge.backend.assessment.dto.ComprehensionSubmitRequest;
import com.merge.backend.assessment.dto.ComprehensionSubmitResponse;
import com.merge.backend.assessment.repository.ComprehensionCheckRepository;
import com.merge.backend.assessment.repository.DrillCompletionRepository;
import com.merge.backend.curriculum.service.ConceptUnlockService;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.progression.domain.ActivityType;
import com.merge.backend.progression.service.ProgressionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class ComprehensionSubmitService {

    private static final int DRILL_PASS_XP = 50;

    private final ComprehensionCheckRepository comprehensionCheckRepository;
    private final DrillCompletionRepository drillCompletionRepository;
    private final StudentRepository studentRepository;
    private final GeminiGateway geminiGateway;
    private final ProgressionService progressionService;
    private final ConceptUnlockService conceptUnlockService;
    private final BuildUnlockService buildUnlockService;

    public ComprehensionSubmitService(ComprehensionCheckRepository comprehensionCheckRepository,
                                      DrillCompletionRepository drillCompletionRepository,
                                      StudentRepository studentRepository,
                                      GeminiGateway geminiGateway,
                                      ProgressionService progressionService,
                                      ConceptUnlockService conceptUnlockService,
                                      BuildUnlockService buildUnlockService) {
        this.comprehensionCheckRepository = comprehensionCheckRepository;
        this.drillCompletionRepository = drillCompletionRepository;
        this.studentRepository = studentRepository;
        this.geminiGateway = geminiGateway;
        this.progressionService = progressionService;
        this.conceptUnlockService = conceptUnlockService;
        this.buildUnlockService = buildUnlockService;
    }

    /**
     * Handles POST /api/v1/comprehension-checks/{id}/submit.
     *
     * Enforcement order:
     * 1. 404 — check not found
     * 2. 403 — authenticated student does not own this check
     * 3. 400 { timerExpired: true } — server clock is past serverDeadline
     *        (client timer is visual only and never used for enforcement)
     * 4. 409 — check already PASSED or FAILED; cannot re-submit
     *
     * On AI pass:
     *   - Saves DrillCompletion (comprehensionPassed=true, judge0Passed=true)
     *   - Awards DRILL_PASS XP via ProgressionService (PR-01)
     *   - Triggers ConceptUnlockService to unlock the next concept if both drills are now passed
     *   - Returns 200 { passed: true, xpAwarded: N }
     *
     * On AI fail:
     *   - Marks check FAILED; student must re-submit the drill to get a new check with fresh questions
     *   - Returns 200 { passed: false }
     */
    @Transactional
    public ComprehensionSubmitResponse submit(Long checkId,
                                              ComprehensionSubmitRequest req,
                                              String studentEmail) {
        ComprehensionCheck check = comprehensionCheckRepository.findById(checkId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Comprehension check not found: " + checkId));

        Student student = studentRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentEmail));

        if (!check.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This comprehension check does not belong to you");
        }

        if (check.getStatus() != ComprehensionCheckStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Comprehension check already " + check.getStatus().name().toLowerCase());
        }

        // Deadline is enforced here on the server. The client timer is purely visual.
        Instant submittedAt = Instant.now();
        if (submittedAt.isAfter(check.getServerDeadline())) {
            check.setStatus(ComprehensionCheckStatus.EXPIRED);
            check.setSubmittedAt(submittedAt);
            comprehensionCheckRepository.save(check);
            throw new ComprehensionTimerExpiredException();
        }

        validateAnswers(req.answers(), check.getQuestions().size());

        // Persist the student's answers before calling AI (idempotency on AI failure)
        check.setAnswers(req.answers());
        check.setSubmittedAt(submittedAt);
        comprehensionCheckRepository.save(check);

        boolean passed = geminiGateway.scoreComprehensionAnswers(new ComprehensionScoreRequest(
                check.getDrillSubmission().getCode(),
                check.getQuestions(),
                req.answers()
        ));

        if (!passed) {
            check.setStatus(ComprehensionCheckStatus.FAILED);
            comprehensionCheckRepository.save(check);
            return ComprehensionSubmitResponse.failed();
        }

        check.setStatus(ComprehensionCheckStatus.PASSED);
        comprehensionCheckRepository.save(check);

        recordDrillCompletion(student, check);

        int xpAwarded = progressionService.awardXp(
                student,
                DRILL_PASS_XP,
                ActivityType.DRILL_PASS,
                student.getCurrentStage(),
                check.getDrill().getId()
        );

        // Unlock next concept if both drills for this concept now have passing comprehension.
        conceptUnlockService.triggerUnlockIfEligible(student, check.getDrill().getConcept());

        // Check both build-unlock conditions simultaneously: all stage drills passed + XP threshold.
        // XP was just awarded above, so student.totalXp is current at the point of this check.
        buildUnlockService.checkAndUnlock(student, student.getCurrentStage());

        return ComprehensionSubmitResponse.passed(xpAwarded);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateAnswers(List<String> answers, int expectedCount) {
        if (answers == null || answers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "answers must not be empty");
        }
        if (answers.size() != expectedCount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Expected " + expectedCount + " answers but received " + answers.size());
        }
        for (int i = 0; i < answers.size(); i++) {
            if (answers.get(i) == null || answers.get(i).isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Answer " + (i + 1) + " must not be empty");
            }
        }
    }

    private void recordDrillCompletion(Student student, ComprehensionCheck check) {
        drillCompletionRepository
                .findByStudentIdAndDrillId(student.getId(), check.getDrill().getId())
                .ifPresentOrElse(
                        existing -> {
                            existing.setComprehensionPassed(true);
                            existing.setJudge0Passed(true);
                            drillCompletionRepository.save(existing);
                        },
                        () -> {
                            DrillCompletion completion = new DrillCompletion();
                            completion.setStudent(student);
                            completion.setDrill(check.getDrill());
                            completion.setJudge0Passed(true);
                            completion.setComprehensionPassed(true);
                            completion.setCompletedAt(Instant.now());
                            drillCompletionRepository.save(completion);
                        }
                );
    }
}
