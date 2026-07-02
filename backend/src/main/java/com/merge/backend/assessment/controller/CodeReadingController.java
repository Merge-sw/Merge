package com.merge.backend.assessment.controller;

import com.merge.backend.assessment.dto.CodeReadingAcceptedResponse;
import com.merge.backend.assessment.dto.CodeReadingRequest;
import com.merge.backend.assessment.service.CodeReadingService;
import com.merge.backend.curriculum.service.ConceptNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/drills")
public class CodeReadingController {

    private final CodeReadingService codeReadingService;

    public CodeReadingController(CodeReadingService codeReadingService) {
        this.codeReadingService = codeReadingService;
    }

    /**
     * POST /api/v1/drills/{drillId}/code-reading
     * Records David's analysis of the unfamiliar codebase (FACT: C — Code Reading).
     * Required gate for Drill 2 only — Drill 1 returns 400.
     * Response { accepted: true } signals the frontend to unlock the code editor.
     * Idempotent: re-submission returns accepted=true without creating a duplicate.
     */
    @PostMapping("/{drillId}/code-reading")
    public ResponseEntity<CodeReadingAcceptedResponse> submit(
            @PathVariable Long drillId,
            @RequestBody CodeReadingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        codeReadingService.submit(drillId, request.response(), userDetails.getUsername());
        return ResponseEntity.ok(CodeReadingAcceptedResponse.ok());
    }

    @ExceptionHandler(ConceptNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ConceptNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
