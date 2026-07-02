package com.merge.backend.curriculum.controller;

import com.merge.backend.curriculum.dto.ResourceCompletionResponse;
import com.merge.backend.curriculum.service.ResourceCompletionService;
import com.merge.backend.curriculum.service.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/resources")
public class ResourceCompletionController {

    private final ResourceCompletionService completionService;

    public ResourceCompletionController(ResourceCompletionService completionService) {
        this.completionService = completionService;
    }

    /**
     * POST /api/v1/resources/{id}/complete
     * First call: records completion, awards up to 5 XP (capped at 50 per stage
     * across all LEARNING_RESOURCE completions), returns { xpAwarded: N }.
     * Repeat call: no XP awarded, returns { xpAwarded: 0, alreadyCompleted: true }.
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ResourceCompletionResponse> complete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        int result = completionService.complete(id, userDetails.getUsername());
        if (result == -1) {
            return ResponseEntity.ok(ResourceCompletionResponse.repeat());
        }
        return ResponseEntity.ok(ResourceCompletionResponse.firstCompletion(result));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
