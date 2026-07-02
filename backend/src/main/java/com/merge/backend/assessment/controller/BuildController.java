package com.merge.backend.assessment.controller;

import com.merge.backend.assessment.domain.Build;
import com.merge.backend.assessment.dto.BuildGeneratingResponse;
import com.merge.backend.assessment.dto.BuildStatusResponse;
import com.merge.backend.assessment.repository.BuildRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/builds")
public class BuildController {

    private final BuildRepository buildRepository;
    private final StudentRepository studentRepository;

    public BuildController(BuildRepository buildRepository,
                           StudentRepository studentRepository) {
        this.buildRepository = buildRepository;
        this.studentRepository = studentRepository;
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
}
