package com.merge.backend.curriculum.controller;

import com.merge.backend.curriculum.dto.StageResponse;
import com.merge.backend.curriculum.service.StageNotFoundException;
import com.merge.backend.curriculum.service.StageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stages")
public class StageController {

    private final StageService stageService;

    public StageController(StageService stageService) {
        this.stageService = stageService;
    }

    /**
     * CU-02: GET /api/v1/stages
     * Returns all five stages with their full configuration.
     */
    @GetMapping
    public ResponseEntity<List<StageResponse>> getAllStages() {
        List<StageResponse> stages = stageService.getAllStages()
                .stream()
                .map(StageResponse::from)
                .toList();
        return ResponseEntity.ok(stages);
    }

    /**
     * CU-02: GET /api/v1/stages/{stageType}
     * Returns configuration for a single stage (SCOUT, CADET, ENGINEER, ARCHITECT, PRINCIPAL).
     */
    @GetMapping("/{stageType}")
    public ResponseEntity<StageResponse> getStage(@PathVariable String stageType) {
        return ResponseEntity.ok(StageResponse.from(stageService.getStage(stageType)));
    }

    @ExceptionHandler(StageNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(StageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
