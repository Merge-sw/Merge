package com.merge.backend.identity.controller;

import com.merge.backend.identity.dto.GeminiTokenRequest;
import com.merge.backend.identity.dto.PromoteResponse;
import com.merge.backend.identity.dto.PromotionStatusResponse;
import com.merge.backend.identity.dto.StudentResponse;
import com.merge.backend.identity.dto.UpdateProfileRequest;
import com.merge.backend.identity.service.GeminiTokenService;
import com.merge.backend.identity.service.InvalidGeminiTokenException;
import com.merge.backend.identity.service.PromotionNotEligibleException;
import com.merge.backend.identity.service.PromotionStatusService;
import com.merge.backend.identity.service.StagePromotionService;
import com.merge.backend.identity.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;
    private final GeminiTokenService geminiTokenService;
    private final PromotionStatusService promotionStatusService;
    private final StagePromotionService stagePromotionService;

    public StudentController(StudentService studentService,
                             GeminiTokenService geminiTokenService,
                             PromotionStatusService promotionStatusService,
                             StagePromotionService stagePromotionService) {
        this.studentService = studentService;
        this.geminiTokenService = geminiTokenService;
        this.promotionStatusService = promotionStatusService;
        this.stagePromotionService = stagePromotionService;
    }

    /**
     * ID-04: GET /api/v1/students/me
     * Returns full student record for the authenticated student.
     * Requires valid JWT.
     */
    @GetMapping("/me")
    public ResponseEntity<StudentResponse> getOwnProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        StudentResponse profile = studentService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    /**
     * ID-04: PUT /api/v1/students/me
     * Updates name and phone for the authenticated student.
     * Requires valid JWT.
     */
    @PutMapping("/me")
    public ResponseEntity<StudentResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        StudentResponse updated = studentService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(updated);
    }

    /**
     * ID-06: POST /api/v1/students/me/gemini-token
     * Validates the supplied Gemini API key against the live Gemini API,
     * then AES-256-GCM encrypts it and persists only the ciphertext.
     * The plaintext token is never returned, logged, or stored in plain form.
     * Requires valid JWT (depends on ID-02).
     */
    @PostMapping("/me/gemini-token")
    public ResponseEntity<StudentResponse> saveGeminiToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GeminiTokenRequest request) {
        StudentResponse response = geminiTokenService.saveToken(
                userDetails.getUsername(), request.token());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/students/me/stage/promotion-status
     * Checks both promotion gates simultaneously for the authenticated student:
     *   1. total_xp >= stage.xp_threshold
     *   2. cumulative build pass score >= stage.build_pass_score_threshold
     *
     * Build pass score = SUM of best overallScore per distinct build with a PASSED submission.
     * Returns exact deficit numbers so the client can display progress to the student.
     */
    @GetMapping("/me/stage/promotion-status")
    public ResponseEntity<PromotionStatusResponse> getPromotionStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                promotionStatusService.check(userDetails.getUsername()));
    }

    /**
     * POST /api/v1/students/me/stage/promote
     * Promotes the authenticated student to the next stage when both gates are met:
     *   1. total_xp >= stage.xp_threshold
     *   2. cumulative build pass score >= stage.build_pass_score_threshold
     *
     * 200 — promoted; returns { fromStage, toStage, xpAtPromotion, buildScoreAtPromotion }
     * 403 — not eligible; returns { missingXp, missingBuildScore } with exact deficits
     * 409 — already promoted from this stage, or already at the highest stage
     */
    @PostMapping("/me/stage/promote")
    public ResponseEntity<PromoteResponse> promote(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(stagePromotionService.promote(userDetails.getUsername()));
    }

    @ExceptionHandler(PromotionNotEligibleException.class)
    public ResponseEntity<Map<String, Integer>> handleNotEligible(PromotionNotEligibleException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "missingXp", ex.getMissingXp(),
                        "missingBuildScore", ex.getMissingBuildScore()));
    }

    @ExceptionHandler(InvalidGeminiTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidGeminiToken(
            InvalidGeminiTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", ex.getMessage()));
    }
}
