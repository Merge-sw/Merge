package com.merge.backend.curriculum.controller;

import com.merge.backend.curriculum.dto.ConceptResponse;
import com.merge.backend.curriculum.dto.ConceptSummary;
import com.merge.backend.curriculum.service.ConceptLockedException;
import com.merge.backend.curriculum.service.ConceptNotFoundException;
import com.merge.backend.curriculum.service.ConceptService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ConceptController {

    private final ConceptService conceptService;

    public ConceptController(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    /**
     * CU-03: GET /api/v1/stages/{stageType}/concepts
     * Returns ordered concept list for the stage.
     * Each concept carries unlocked=true/false based on whether the authenticated
     * student's current stage is at or past the requested stage.
     */
    @GetMapping("/api/v1/stages/{stageType}/concepts")
    public ResponseEntity<List<ConceptSummary>> getConceptsForStage(
            @PathVariable String stageType,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                conceptService.getConceptsForStage(stageType, userDetails.getUsername()));
    }

    /**
     * CU-03: GET /api/v1/concepts/{id}
     * Returns full concept metadata including failure_scenario.
     * Returns 403 if the concept belongs to a stage the student has not reached yet.
     */
    @GetMapping("/api/v1/concepts/{id}")
    public ResponseEntity<ConceptResponse> getConcept(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                ConceptResponse.from(conceptService.getConcept(id, userDetails.getUsername())));
    }

    @ExceptionHandler(ConceptNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ConceptNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ConceptLockedException.class)
    public ResponseEntity<Map<String, String>> handleLocked(ConceptLockedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }
}
