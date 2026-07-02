package com.merge.backend.assessment.controller;

import com.merge.backend.assessment.dto.DrillSubmitRequest;
import com.merge.backend.assessment.dto.DrillSubmitResponse;
import com.merge.backend.assessment.service.DrillSubmissionService;
import com.merge.backend.assessment.service.DuplicateSubmissionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/drills")
public class DrillSubmissionController {

    private final DrillSubmissionService submissionService;

    public DrillSubmissionController(DrillSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /**
     * POST /api/v1/drills/{id}/submit
     * Validation order (all enforced before Judge0):
     *   400 — code blank, testSuite blank, architectureAnswers incomplete, idempotencyKey missing
     *   409 — duplicate idempotency key (body contains originalSubmission)
     *   404 — drill not found
     *   403 — student does not own drill, or drill is locked
     * On success returns 200 with submissionId and PENDING status.
     * Judge0 execution is async and updates status downstream.
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<DrillSubmitResponse> submit(
            @PathVariable Long id,
            @RequestBody DrillSubmitRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                submissionService.submit(id, request, userDetails.getUsername()));
    }

    @ExceptionHandler(DuplicateSubmissionException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateSubmissionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("originalSubmission", ex.getOriginalSubmission()));
    }
}
