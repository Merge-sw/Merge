package com.merge.backend.assessment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merge.backend.ai.gateway.GeminiGateway;
import com.merge.backend.assessment.domain.*;
import com.merge.backend.assessment.dto.*;
import com.merge.backend.assessment.dto.CleanCodeReviewResult;
import com.merge.backend.assessment.exception.DuplicateBuildSubmissionException;
import com.merge.backend.assessment.repository.BuildGateResultRepository;
import com.merge.backend.assessment.repository.BuildRepository;
import com.merge.backend.assessment.repository.BuildSubmissionRepository;
import com.merge.backend.curriculum.domain.Stage;
import com.merge.backend.curriculum.repository.StageRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.assessment.domain.BuildComprehensionCheck;
import com.merge.backend.assessment.judge0.Judge0ExecutionService;
import com.merge.backend.assessment.judge0.Judge0Outcome;
import com.merge.backend.infrastructure.queue.JobQueueService;
import com.merge.backend.infrastructure.queue.JobType;
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

    private final BuildRepository buildRepository;
    private final BuildSubmissionRepository buildSubmissionRepository;
    private final BuildGateResultRepository buildGateResultRepository;
    private final StudentRepository studentRepository;
    private final StageRepository stageRepository;
    private final Judge0ExecutionService judge0ExecutionService;
    private final BuildTestSuiteValidator testSuiteValidator;
    private final BuildComprehensionTriggerService comprehensionTriggerService;
    private final GeminiGateway geminiGateway;
    private final ProgressionService progressionService;
    private final JobQueueService jobQueueService;
    private final ObjectMapper objectMapper;

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
            throw new DuplicateBuildSubmissionException(BuildSubmitResponse.from(existing, gates, null));
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

        boolean gate1Passed = Boolean.TRUE.equals(submission.getGate1Passed());
        boolean gate2Passed = Boolean.TRUE.equals(submission.getGate2Passed());
        boolean gate3Passed = Boolean.TRUE.equals(submission.getGate3Passed());

        Long comprehensionCheckId = null;

        if (!gate1Passed || !gate2Passed) {
            // Gates 1 and 2 are hard requirements — no tier, no XP.
            submission.setOverallStatus(BuildSubmissionStatus.FAILED);

        } else if (!gate3Passed) {
            // MINIMUM tier: gates 1+2 passed, gate 3 failed. Award 150 XP × decay immediately.
            // The comprehension check is not triggered; this is the terminal evaluation point.
            int xp = progressionService.awardXp(student.getId(),
                    BuildPassTier.MINIMUM.computeXp(attemptNumber),
                    ActivityType.BUILD_PASS, build.getStageName(), buildId).awarded();
            submission.setTier(BuildPassTier.MINIMUM.name());
            submission.setXpAwarded(xp);
            submission.setOverallStatus(BuildSubmissionStatus.PASSED);
            enqueuePassJobs(submission);

        } else {
            // Gates 1+2+3 all passed — trigger comprehension (gate 4).
            // Overall status stays PENDING until the student submits comprehension answers.
            BuildComprehensionCheck check = comprehensionTriggerService.triggerFor(submission);
            comprehensionCheckId = check.getId();
        }

        buildSubmissionRepository.save(submission);
        buildGateResultRepository.saveAll(gateResults);

        List<BuildGateResultDto> gateDtos = gateResults.stream()
                .map(BuildGateResultDto::from).toList();
        return BuildSubmitResponse.from(submission, gateDtos, comprehensionCheckId);
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

        // Gate 3 — CleanCodeReviewer (AI-07): scores code against stage rubric.
        // Rubric: Cadet=naming only, Engineer=+size+redundancy, Architect=full SOLID.
        // Pass condition: AI score >= Stage.cleanCodeMinScore (stages.clean_code_min_score).
        BuildGateResult cleanCodeResult = findGate(gateResults, BuildGate.CLEAN_CODE);
        boolean gate3Passed = false;
        int cleanCodeScore = 0;
        try {
            CleanCodeReviewResult review = geminiGateway.reviewBuildCleanCode(
                    new BuildCleanCodeReviewRequest(
                            request.code(),
                            stage.getName(),
                            stage.getCleanCodeLevel()
                    ));
            cleanCodeScore = review.score();
            gate3Passed = cleanCodeScore >= stage.getCleanCodeMinScore();
            if (gate3Passed) {
                markPassed(cleanCodeResult, review.feedback());
            } else {
                String threshold = "minimum: " + stage.getCleanCodeMinScore()
                        + ", your score: " + cleanCodeScore;
                markFailed(cleanCodeResult,
                        (review.feedback() != null ? review.feedback() + " — " : "") + threshold);
            }
        } catch (Exception e) {
            log.warn("Clean code gate threw exception for submission {}: {}", submission.getId(), e.getMessage());
            markFailed(cleanCodeResult, "Clean code review unavailable");
        }
        submission.setGate3Passed(gate3Passed);
        submission.setOverallScore(cleanCodeScore);
        buildSubmissionRepository.saveAndFlush(submission);

        // Gates 4–5 (ARCHITECTURE, COMPETENCY_SIGNAL) run after Gate 4 comprehension passes.
        // They are deferred to future gate tickets and not executed here.
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

    /**
     * pendingXp shows the student the maximum potential XP (DISTINCTION tier × attempt decay).
     * Actual XP awarded at terminal evaluation time depends on which tier is achieved.
     */
    private static int calculatePendingXp(int attemptNumber) {
        return BuildPassTier.DISTINCTION.computeXp(attemptNumber);
    }

    private void enqueuePassJobs(BuildSubmission submission) {
        try {
            String githubPayload = objectMapper.writeValueAsString(
                    new GithubCommitJobPayload(submission.getId(),
                            submission.getStudent().getId(),
                            submission.getBuild().getId()));
            jobQueueService.enqueue(JobType.GITHUB_COMMIT, githubPayload);

            String competencyPayload = objectMapper.writeValueAsString(
                    new CompetencySignalJobPayload(submission.getId(),
                            submission.getStudent().getId(),
                            submission.getBuild().getId()));
            jobQueueService.enqueue(JobType.COMPETENCY_SIGNAL, competencyPayload);
        } catch (JsonProcessingException e) {
            log.error("Failed to enqueue pass jobs for submission {}: {}", submission.getId(), e.getMessage());
        }
    }
}
