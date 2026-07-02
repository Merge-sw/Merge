package com.merge.backend.engagement.service;

import com.merge.backend.engagement.domain.Season;

import java.time.Instant;
import java.util.List;

public interface SeasonService {
    Season createSeason(String name, Instant startDate, Instant endDate, boolean active);
    Season closeSeason(Long id);
    List<Season> getAllSeasons();
}
