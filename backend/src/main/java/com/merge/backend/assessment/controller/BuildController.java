package com.merge.backend.assessment.controller;

import com.merge.backend.assessment.domain.Build;
import com.merge.backend.assessment.dto.BuildGeneratingResponse;
import com.merge.backend.assessment.dto.BuildStatusResponse;
import com.merge.backend.assessment.dto.BuildSubmitRequest;
import com.merge.backend.assessment.dto.BuildSubmitResponse;
import com.merge.backend.assessment.exception.DuplicateBuildSubmissionException;
import com.merge.backend.assessment.repository.BuildRepository;
import com.merge.backend.assessment.service.BuildSubmitService;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/builds")
public class BuildController {

    private final BuildRepository buildRepository;
    private final StudentRepository studentRepository;
    private final BuildSubmitService buildSubmitService;

    public BuildController(BuildRepository buildRepository,
                           StudentRepository studentRepository,
                           BuildSubmitService buildSubmitService) {
        this.buildRepository = buildRepository;
        this.studentRepository = studentRepository;
        this.buildSubmitService = buildSubmitService;
    }

    @ExceptionHandler(DuplicateBuildSubmissionException.class)
    public ResponseEntity<BuildSubmitResponse> handleDuplicateSubmission(DuplicateBuildSubmissionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getOriginalSubmission());
    }

    /**
     * GET /api/v1/builds/current
     *
     * Returns the AI-03-generated build for the student's current stage.
     * The PRD and all related fields are personalised to this student's specific
     * Drill performance history — not generic templates.
     *
     * Responses:
     *   403 — build not yet unlocked (XP threshold or drill conditions not yet met)
     *   202 { generating: true } — unlocked but BUILD_PRD_GENERATION job not yet complete;
     *         client should poll until 200 is received
     *   200 { buildId, stageName, unlockedAt, prd, requirements, constraints, sfiaCompetencies }
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrent(@AuthenticationPrincipal UserDetails userDetails) {

        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Build build = buildRepository
                .findByStudentIdAndStageName(student.getId(), student.getCurrentStage())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Build not yet unlocked for stage " + student.getCurrentStage()));

        if (build.getPrd() == null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(BuildGeneratingResponse.instance());
        }

        return ResponseEntity.ok(BuildStatusResponse.from(build));
    }

    /**
     * POST /api/v1/builds/{id}/submit
     *
     * Triggers all five gates sequentially:
     *   1. JUDGE0          — code executes and tests pass
     *   2. ARCHITECTURE    — architecture document reviewed against PRD requirements/constraints
     *   3. CLEAN_CODE      — code quality evaluated at the stage's clean-code level
     *   4. TEST_QUALITY    — test suite coverage and meaningfulness reviewed
     *   5. COMPETENCY_SIGNAL — SFIA competency signals assessed across all artefacts
     *
     * XP decay by attempt: 1st 200 (100%), 2nd 150 (75%), 3rd 100 (50%), 4th+ 50 (25%).
     * XP is awarded only when all five gates pass.
     *
     * Responses:
     *   400 — validation failure (missing/blank required field)
     *   403 — build not owned by student, not unlocked, or PRD still generating
     *   404 — build not found
     *   409 — duplicate idempotency key (body = original submission response)
     *   200 — all gates evaluated; body contains per-gate results and xpAwarded
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<BuildSubmitResponse> submit(
            @PathVariable Long id,
            @RequestBody BuildSubmitRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        BuildSubmitResponse response = buildSubmitService.submit(id, request, userDetails);
        return ResponseEntity.ok(response);
    }
}
