package com.merge.backend.assessment.controller;

import com.merge.backend.assessment.dto.ComprehensionSubmitRequest;
import com.merge.backend.assessment.dto.ComprehensionSubmitResponse;
import com.merge.backend.assessment.service.ComprehensionSubmitService;
import com.merge.backend.assessment.service.ComprehensionTimerExpiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/comprehension-checks")
public class ComprehensionCheckController {

    private final ComprehensionSubmitService submitService;

    public ComprehensionCheckController(ComprehensionSubmitService submitService) {
        this.submitService = submitService;
    }

    /**
     * POST /api/v1/comprehension-checks/{id}/submit
     *
     * Deadline is enforced server-side — the client timer is purely visual.
     * Responses:
     *   400 { timerExpired: true }  — server deadline exceeded
     *   403                         — student does not own this check
     *   404                         — check not found
     *   409                         — already passed or failed (cannot re-submit)
     *   200 { passed: true,  xpAwarded: N }  — comprehension passed; XP awarded; next concept may unlock
     *   200 { passed: false }               — comprehension failed; student re-tries the drill
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<ComprehensionSubmitResponse> submit(
            @PathVariable Long id,
            @RequestBody ComprehensionSubmitRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                submitService.submit(id, request, userDetails.getUsername()));
    }

    @ExceptionHandler(ComprehensionTimerExpiredException.class)
    public ResponseEntity<Map<String, Boolean>> handleTimerExpired(ComprehensionTimerExpiredException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("timerExpired", true));
    }
}
