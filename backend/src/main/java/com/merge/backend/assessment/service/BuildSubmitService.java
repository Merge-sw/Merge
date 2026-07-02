package com.merge.backend.assessment.service;

import com.merge.backend.ai.gateway.GeminiGateway;
import com.merge.backend.assessment.domain.*;
import com.merge.backend.assessment.dto.*;
import com.merge.backend.assessment.exception.DuplicateBuildSubmissionException;
import com.merge.backend.assessment.repository.BuildGateResultRepository;
import com.merge.backend.assessment.repository.BuildRepository;
import com.merge.backend.assessment.repository.BuildSubmissionRepository;
import com.merge.backend.curriculum.domain.Stage;
import com.merge.backend.curriculum.repository.StageRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.assessment.judge0.Judge0ExecutionService;
import com.merge.backend.assessment.judge0.Judge0Outcome;
import com.merge.backend.progression.domain.ActivityType;
import com.merge.backend.progression.service.ProgressionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildSubmitService {

    private static final int BASE_BUILD_XP = 200;

    private final BuildRepository buildRepository;
    private final BuildSubmissionRepository buildSubmissionRepository;
    private final BuildGateResultRepository buildGateResultRepository;
    private final StudentRepository studentRepository;
    private final StageRepository stageRepository;
    private final Judge0ExecutionService judge0ExecutionService;
    private final BuildTestSuiteValidator testSuiteValidator;
    private final GeminiGateway geminiGateway;
    private final ProgressionService progressionService;

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public BuildSubmitResponse submit(Long buildId, BuildSubmitRequest request, UserDetails userDetails) {
        List<String> errors = new ArrayList<>();
        if (request.code() == null || request.code().isBlank()) errors.add("code is required");
        if (request.testSuite() == null || request.testSuite().isBlank()) errors.add("testSuite is required");
        if (request.architectureDocument() == null || request.architectureDocument().isBlank())
            errors.add("architectureDocument is required");
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank())
            errors.add("idempotencyKey is required");
        if (!errors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("; ", errors));
        }

        buildSubmissionRepository.findByIdempotencyKey(request.idempotencyKey()).ifPresent(existing -> {
            List<BuildGateResultDto> gates = buildGateResultRepository
                    .findByBuildSubmissionIdOrderByGateAsc(existing.getId())
                    .stream().map(BuildGateResultDto::from).toList();
            throw new DuplicateBuildSubmissionException(BuildSubmitResponse.from(existing, gates));
        });
        return doSubmit(buildId, request, userDetails);
    }

    private BuildSubmitResponse doSubmit(Long buildId, BuildSubmitRequest request, UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        Build build = buildRepository.findById(buildId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Build not found"));

        if (!build.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This build belongs to another student");
        }
        if (!build.isUnlocked()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Build is not yet unlocked");
        }
        if (build.getPrd() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Build PRD is still generating");
        }
        if (build.getHiddenTestSuite() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Build hidden tests are not yet available");
        }

        Stage stage = stageRepository.findById(build.getStageName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Stage data missing for " + build.getStageName()));

        int previousAttempts = buildSubmissionRepository.countByStudentIdAndBuildId(student.getId(), buildId);
        int attemptNumber = previousAttempts + 1;
        int pendingXp = calculatePendingXp(attemptNumber);

        BuildSubmission submission = new BuildSubmission();
        submission.setBuild(build);
        submission.setStudent(student);
        submission.setCode(request.code());
        submission.setTestSuite(request.testSuite());
        submission.setArchitectureDocument(request.architectureDocument());
        submission.setIdempotencyKey(request.idempotencyKey());
        submission.setAttemptNumber(attemptNumber);
        submission.setOverallStatus(BuildSubmissionStatus.PENDING);
        submission.setPendingXp(pendingXp);
        submission.setSubmittedAt(Instant.now());
        submission = buildSubmissionRepository.saveAndFlush(submission);

        List<BuildGateResult> gateResults = initGateResults(submission);
        buildGateResultRepository.saveAllAndFlush(gateResults);

        runGates(submission, gateResults, build, stage, request);

        boolean allGatesPassed = gateResults.stream()
                .allMatch(g -> g.getStatus() == BuildGateStatus.PASSED);

        // Gate 1 is a hard blocker: XP is never awarded unless hidden-test execution passed,
        // regardless of other gates' outcomes.
        if (Boolean.TRUE.equals(submission.getGate1Passed()) && allGatesPassed) {
            submission.setOverallStatus(BuildSubmissionStatus.PASSED);
            int awarded = progressionService.awardXp(student, pendingXp, ActivityType.BUILD_PASS,
                    build.getStageName(), buildId);
            submission.setXpAwarded(awarded);
        } else {
            submission.setOverallStatus(BuildSubmissionStatus.FAILED);
        }

        buildSubmissionRepository.save(submission);
        buildGateResultRepository.saveAll(gateResults);

        List<BuildGateResultDto> gateDtos = gateResults.stream()
                .map(BuildGateResultDto::from).toList();
        return BuildSubmitResponse.from(submission, gateDtos);
    }

    private void runGates(BuildSubmission submission, List<BuildGateResult> gateResults,
                          Build build, Stage stage, BuildSubmitRequest request) {

        // Gate 1 — Judge0 against hidden test suite (same config as Drill execution).
        // Hidden test content is NEVER included in any feedback returned to the student.
        BuildGateResult judge0Result = findGate(gateResults, BuildGate.JUDGE0);
        boolean gate1Passed = false;
        try {
            Judge0Outcome outcome = judge0ExecutionService.execute(request.code(), build.getHiddenTestSuite());
            gate1Passed = outcome.testsPassed();
            if (gate1Passed) {
                markPassed(judge0Result, null);
            } else {
                markFailed(judge0Result, hiddenTestFeedback(outcome));
            }
        } catch (Exception e) {
            log.warn("Judge0 gate threw exception for submission {}: {}", submission.getId(), e.getMessage());
            markFailed(judge0Result, "Code execution failed");
        }
        submission.setGate1Passed(gate1Passed);
        buildSubmissionRepository.saveAndFlush(submission);

        // Gate 2 — Student's own test suite executed via Judge0.
        // Static quality checks (empty, trivial, Cadet minimum) run first to avoid
        // burning Judge0 quota on suites that are structurally invalid.
        BuildGateResult testQualityResult = findGate(gateResults, BuildGate.TEST_QUALITY);
        boolean gate2Passed = false;
        try {
            BuildTestSuiteValidator.ValidationResult validation =
                    testSuiteValidator.validate(request.testSuite(), stage.getName());
            if (!validation.valid()) {
                markFailed(testQualityResult, validation.errorMessage());
            } else {
                Judge0Outcome studentOutcome = judge0ExecutionService.execute(
                        request.code(), request.testSuite());
                gate2Passed = studentOutcome.testsPassed();
                if (gate2Passed) {
                    markPassed(testQualityResult, null);
                } else {
                    markFailed(testQualityResult, studentTestFeedback(studentOutcome));
                }
            }
        } catch (Exception e) {
            log.warn("Student test gate threw exception for submission {}: {}", submission.getId(), e.getMessage());
            markFailed(testQualityResult, "Test execution failed");
        }
        submission.setGate2Passed(gate2Passed);
        buildSubmissionRepository.saveAndFlush(submission);

        // Gate 3 — Architecture review
        BuildGateResult archResult = findGate(gateResults, BuildGate.ARCHITECTURE);
        try {
            boolean passed = geminiGateway.reviewBuildArchitecture(new BuildArchitectureReviewRequest(
                    request.architectureDocument(),
                    build.getPrd(),
                    build.getRequirements(),
                    build.getConstraints()
            ));
            if (passed) {
                markPassed(archResult, null);
            } else {
                markFailed(archResult, "Architecture document does not sufficiently address the PRD requirements");
            }
        } catch (Exception e) {
            log.warn("Architecture gate threw exception for submission {}: {}", submission.getId(), e.getMessage());
            markFailed(archResult, "Architecture review unavailable");
        }

        // Gate 3 — Clean code
        BuildGateResult cleanCodeResult = findGate(gateResults, BuildGate.CLEAN_CODE);
        try {
            boolean passed = geminiGateway.reviewBuildCleanCode(new BuildCleanCodeReviewRequest(
                    request.code(),
                    stage.getName(),
                    stage.getCleanCodeLevel()
            ));
            if (passed) {
                markPassed(cleanCodeResult, null);
            } else {
                markFailed(cleanCodeResult, "Code does not meet the " + stage.getCleanCodeLevel() + " clean-code standard for this stage");
            }
        } catch (Exception e) {
            log.warn("Clean code gate threw exception for submission {}: {}", submission.getId(), e.getMessage());
            markFailed(cleanCodeResult, "Clean code review unavailable");
        }

        // Gate 5 — Competency signal
        BuildGateResult competencyResult = findGate(gateResults, BuildGate.COMPETENCY_SIGNAL);
        try {
            boolean passed = geminiGateway.evaluateBuildCompetencySignal(new BuildCompetencySignalRequest(
                    request.code(),
                    request.testSuite(),
                    request.architectureDocument(),
                    build.getSfiaCompetencies()
            ));
            if (passed) {
                markPassed(competencyResult, null);
            } else {
                markFailed(competencyResult, "Submission does not demonstrate the required SFIA competency signals");
            }
        } catch (Exception e) {
            log.warn("Competency gate threw exception for submission {}: {}", submission.getId(), e.getMessage());
            markFailed(competencyResult, "Competency evaluation unavailable");
        }
    }

    private List<BuildGateResult> initGateResults(BuildSubmission submission) {
        List<BuildGateResult> results = new ArrayList<>();
        for (BuildGate gate : BuildGate.values()) {
            BuildGateResult r = new BuildGateResult();
            r.setBuildSubmission(submission);
            r.setGate(gate);
            r.setStatus(BuildGateStatus.PENDING);
            results.add(r);
        }
        return results;
    }

    private BuildGateResult findGate(List<BuildGateResult> results, BuildGate gate) {
        return results.stream().filter(r -> r.getGate() == gate).findFirst()
                .orElseThrow(() -> new IllegalStateException("Gate result missing: " + gate));
    }

    private void markPassed(BuildGateResult result, String feedback) {
        result.setStatus(BuildGateStatus.PASSED);
        result.setFeedback(feedback);
        result.setEvaluatedAt(Instant.now());
    }

    private void markFailed(BuildGateResult result, String feedback) {
        result.setStatus(BuildGateStatus.FAILED);
        result.setFeedback(feedback);
        result.setEvaluatedAt(Instant.now());
    }

    /**
     * Gate 2 feedback: student's own tests failed against their own code.
     * No hidden content is at risk here — stderr is safe to surface in full.
     */
    private String studentTestFeedback(Judge0Outcome outcome) {
        return switch (outcome.statusId()) {
            case 4 -> "Not all submitted tests pass against your code"
                    + (outcome.stderr() != null ? ": " + outcome.stderr() : "");
            case 5 -> "Test execution timed out";
            case 6 -> "Compilation error" + (outcome.stderr() != null ? ": " + outcome.stderr() : "");
            default -> "Runtime error" + (outcome.stderr() != null ? ": " + outcome.stderr() : "");
        };
    }

    /**
     * Maps a Judge0 failure outcome to student-facing feedback without revealing hidden test content.
     * Status 4 (wrong answer) is intentionally vague — test cases must remain hidden.
     * Compile and runtime errors reference the student's own code, so stderr is safe to show.
     */
    private String hiddenTestFeedback(Judge0Outcome outcome) {
        return switch (outcome.statusId()) {
            case 4 -> "Your code did not pass the hidden test suite";
            case 5 -> "Code execution timed out against the hidden test suite";
            case 6 -> "Compilation error" + (outcome.stderr() != null ? ": " + outcome.stderr() : "");
            default -> "Runtime error" + (outcome.stderr() != null ? ": " + outcome.stderr() : "");
        };
    }

    private int calculatePendingXp(int attemptNumber) {
        return switch (attemptNumber) {
            case 1 -> BASE_BUILD_XP;
            case 2 -> (int) (BASE_BUILD_XP * 0.75);
            case 3 -> (int) (BASE_BUILD_XP * 0.50);
            default -> (int) (BASE_BUILD_XP * 0.25);
        };
    }
}
