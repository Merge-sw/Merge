package com.merge.backend.curriculum.controller;

import com.merge.backend.curriculum.dto.SyntaxExerciseResponse;
import com.merge.backend.curriculum.dto.SyntaxExerciseSubmitRequest;
import com.merge.backend.curriculum.dto.SyntaxExerciseSubmitResponse;
import com.merge.backend.curriculum.service.ConceptNotFoundException;
import com.merge.backend.curriculum.service.SyntaxExerciseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class SyntaxExerciseController {

    private final SyntaxExerciseService syntaxExerciseService;

    public SyntaxExerciseController(SyntaxExerciseService syntaxExerciseService) {
        this.syntaxExerciseService = syntaxExerciseService;
    }

    /**
     * GET /api/v1/concepts/{id}/syntax-exercise
     * Returns the syntax exercise for a concept. Only available when the student's
     * current stage is CADET. Returns 404 for all other stages (Scout, Engineer+)
     * so the resource appears non-existent rather than forbidden.
     */
    @GetMapping("/api/v1/concepts/{id}/syntax-exercise")
    public ResponseEntity<SyntaxExerciseResponse> getExercise(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                SyntaxExerciseResponse.from(
                        syntaxExerciseService.getForConcept(id, userDetails.getUsername())));
    }

    /**
     * POST /api/v1/syntax-exercises/{id}/submit
     * Records a submission and returns whether the student's code matches the solution.
     * No XP is awarded — syntax exercises are unassessed practice.
     */
    @PostMapping("/api/v1/syntax-exercises/{id}/submit")
    public ResponseEntity<SyntaxExerciseSubmitResponse> submit(
            @PathVariable Long id,
            @RequestBody SyntaxExerciseSubmitRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean correct = syntaxExerciseService.submit(id, request.submittedCode(),
                userDetails.getUsername());
        return ResponseEntity.ok(new SyntaxExerciseSubmitResponse(correct));
    }

    @ExceptionHandler(ConceptNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ConceptNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
