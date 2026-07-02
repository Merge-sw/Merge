package com.merge.backend.assessment.controller;

import com.merge.backend.assessment.domain.BuildComprehensionCheck;
import com.merge.backend.assessment.dto.BuildComprehensionCheckResponse;
import com.merge.backend.assessment.dto.BuildComprehensionSubmitRequest;
import com.merge.backend.assessment.dto.BuildComprehensionSubmitResponse;
import com.merge.backend.assessment.exception.BuildComprehensionTimerExpiredException;
import com.merge.backend.assessment.repository.BuildComprehensionCheckRepository;
import com.merge.backend.assessment.service.BuildComprehensionSubmitService;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/build-comprehension")
public class BuildComprehensionController {

    private final BuildComprehensionCheckRepository checkRepository;
    private final StudentRepository studentRepository;
    private final BuildComprehensionSubmitService submitService;

    public BuildComprehensionController(BuildComprehensionCheckRepository checkRepository,
                                        StudentRepository studentRepository,
                                        BuildComprehensionSubmitService submitService) {
        this.checkRepository = checkRepository;
        this.studentRepository = studentRepository;
        this.submitService = submitService;
    }

    @ExceptionHandler(BuildComprehensionTimerExpiredException.class)
    public ResponseEntity<Map<String, Boolean>> handleTimerExpired(
            BuildComprehensionTimerExpiredException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("timerExpired", true));
    }

    /**
     * GET /api/v1/build-comprehension/{id}
     *
     * Returns the questions and deadline for a build comprehension check.
     * Used by the client to render the timed question screen after build submission.
     * Ownership-checked: student may only view their own checks.
     */
    @GetMapping("/{id}")
    public BuildComprehensionCheckResponse getCheck(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        BuildComprehensionCheck check = checkRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Build comprehension check not found"));

        if (!check.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This comprehension check belongs to another student");
        }

        return BuildComprehensionCheckResponse.from(check);
    }

    /**
     * POST /api/v1/build-comprehension/{id}/submit { answers }
     *
     * Submits answers to a build comprehension check.
     * serverDeadline is enforced server-side; client timer is purely visual.
     * On deadline exceeded: 400 { timerExpired: true }, submission marked FAILED.
     * On AI score fail: 200 { passed: false } — student must resubmit the build.
     * On pass: 200 { passed: true, xpAwarded: N }, gate4_passed set, XP credited.
     */
    @PostMapping("/{id}/submit")
    public BuildComprehensionSubmitResponse submit(
            @PathVariable Long id,
            @RequestBody BuildComprehensionSubmitRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return submitService.submit(id, request, userDetails);
    }
}
