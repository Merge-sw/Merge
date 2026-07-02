package com.merge.backend.assessment.controller;

import com.merge.backend.assessment.domain.Build;
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
     * Returns the build record for the authenticated student's current stage.
     *
     * Responses:
     *   404 — build not yet unlocked (both conditions not met)
     *   202 — build unlocked but PRD generation job has not yet completed (prd is null)
     *         Client should poll; timer/spinner is appropriate here
     *   200 { buildId, stageName, status: "READY", unlockedAt, prd } — PRD ready
     */
    @GetMapping("/current")
    public ResponseEntity<BuildStatusResponse> getCurrent(
            @AuthenticationPrincipal UserDetails userDetails) {

        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Build build = buildRepository
                .findByStudentIdAndStageName(student.getId(), student.getCurrentStage())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Build not yet unlocked for stage " + student.getCurrentStage()));

        BuildStatusResponse body = BuildStatusResponse.from(build);

        if (build.getPrd() == null) {
            // PRD generation job queued but not yet complete — client polls
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
        }

        return ResponseEntity.ok(body);
    }
}
