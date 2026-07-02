package com.merge.backend.engagement.controller;

import com.merge.backend.engagement.dto.CohortMomentumEntry;
import com.merge.backend.engagement.service.CohortMomentumService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cohort")
public class CohortController {

    private final CohortMomentumService cohortMomentumService;

    public CohortController(CohortMomentumService cohortMomentumService) {
        this.cohortMomentumService = cohortMomentumService;
    }

    /**
     * GET /api/v1/cohort/momentum
     * Returns all students in the cohort with their current momentum state, XP total, and initials.
     * Full names are never included — initials only.
     * Accessible to any authenticated cohort member.
     */
    @GetMapping("/momentum")
    public ResponseEntity<List<CohortMomentumEntry>> getMomentum() {
        return ResponseEntity.ok(cohortMomentumService.getCohortMomentum());
    }
}
