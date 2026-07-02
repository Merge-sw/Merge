package com.merge.backend.engagement.service;

import com.merge.backend.engagement.domain.Season;
import com.merge.backend.engagement.repository.SeasonRepository;
import com.merge.backend.engagement.repository.WeeklyMomentumRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional
public class SeasonServiceImpl implements SeasonService {

    private final SeasonRepository seasonRepository;
    private final WeeklyMomentumRepository weeklyMomentumRepository;

    public SeasonServiceImpl(SeasonRepository seasonRepository,
                             WeeklyMomentumRepository weeklyMomentumRepository) {
        this.seasonRepository = seasonRepository;
        this.weeklyMomentumRepository = weeklyMomentumRepository;
    }

    @Override
    public Season createSeason(String name, Instant startDate, Instant endDate, boolean active) {
        if (active) {
            // Deactivate any currently active season
            seasonRepository.findByActive(true).ifPresent(s -> {
                s.setActive(false);
                seasonRepository.save(s);
            });
        }
        Season season = new Season();
        season.setName(name);
        season.setStartDate(startDate);
        season.setEndDate(endDate);
        season.setActive(active);
        season.setClosed(false);
        return seasonRepository.save(season);
    }

    @Override
    public Season closeSeason(Long id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Season not found with id: " + id));
        
        if (season.isClosed()) {
            throw new IllegalStateException("Season is already closed.");
        }

        season.setActive(false);
        season.setClosed(true);
        Season saved = seasonRepository.save(season);

        // Convert Instants to LocalDate in UTC timezone
        LocalDate startLocalDate = LocalDate.ofInstant(season.getStartDate(), ZoneOffset.UTC);
        LocalDate endLocalDate = LocalDate.ofInstant(season.getEndDate(), ZoneOffset.UTC);

        // Lock all weekly momentum records within this season date range
        weeklyMomentumRepository.lockAllForDateRange(startLocalDate, endLocalDate);

        return saved;
    }

    @Override
    public List<Season> getAllSeasons() {
        return seasonRepository.findAll();
    }
}
