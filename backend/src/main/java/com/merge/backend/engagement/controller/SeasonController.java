package com.merge.backend.engagement.controller;

import com.merge.backend.engagement.domain.Season;
import com.merge.backend.engagement.dto.SeasonRequest;
import com.merge.backend.engagement.service.SeasonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seasons")
public class SeasonController {

    private final SeasonService seasonService;

    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @PostMapping
    public ResponseEntity<Season> createSeason(@RequestBody SeasonRequest request) {
        Season season = seasonService.createSeason(
                request.name(),
                request.startDate(),
                request.endDate(),
                request.active()
        );
        return ResponseEntity.ok(season);
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Season> closeSeason(@PathVariable Long id) {
        Season season = seasonService.closeSeason(id);
        return ResponseEntity.ok(season);
    }

    @GetMapping
    public ResponseEntity<List<Season>> getAllSeasons() {
        return ResponseEntity.ok(seasonService.getAllSeasons());
    }
}
