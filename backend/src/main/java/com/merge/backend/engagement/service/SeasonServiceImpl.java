package com.merge.backend.engagement.service;

import com.merge.backend.engagement.domain.Season;
import com.merge.backend.engagement.domain.SeasonBadge;
import com.merge.backend.engagement.repository.SeasonBadgeRepository;
import com.merge.backend.engagement.repository.SeasonRepository;
import com.merge.backend.engagement.repository.WeeklyMomentumRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.progression.domain.ActivityType;
import com.merge.backend.progression.service.ProgressionService;
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
    private final StudentRepository studentRepository;
    private final SeasonBadgeRepository seasonBadgeRepository;
    private final ProgressionService progressionService;

    public SeasonServiceImpl(SeasonRepository seasonRepository,
                             WeeklyMomentumRepository weeklyMomentumRepository,
                             StudentRepository studentRepository,
                             SeasonBadgeRepository seasonBadgeRepository,
                             ProgressionService progressionService) {
        this.seasonRepository = seasonRepository;
        this.weeklyMomentumRepository = weeklyMomentumRepository;
        this.studentRepository = studentRepository;
        this.seasonBadgeRepository = seasonBadgeRepository;
        this.progressionService = progressionService;
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

    @Override
    public void lockSeasonAndAwardBadges(Long seasonId) {
        // 1. Close/lock the season
        Season season = closeSeason(seasonId);

        // 2. Fetch all students
        List<Student> students = studentRepository.findAll();
        if (students.isEmpty()) {
            return;
        }

        // 3. Sort students by total XP descending
        List<Student> sorted = students.stream()
                .sorted((s1, s2) -> s2.getTotalXp().compareTo(s1.getTotalXp()))
                .toList();

        int totalStudents = sorted.size();

        // 4. Calculate rank, percentile, and award badges/XP
        for (int i = 0; i < totalStudents; i++) {
            Student student = sorted.get(i);
            int rank = i + 1;
            double percentile = (double) rank / totalStudents;

            String badgeType = null;
            int xpAward = 0;

            if (percentile <= 0.10) {
                badgeType = "GOLD";
                xpAward = 200;
            } else if (percentile <= 0.25) {
                badgeType = "SILVER";
                xpAward = 100;
            }

            if (badgeType != null) {
                // Insert season badge record
                SeasonBadge badge = new SeasonBadge();
                badge.setStudent(student);
                badge.setSeason(season);
                badge.setBadgeType(badgeType);
                badge.setRank(rank);
                badge.setPercentile(percentile);
                badge.setXpAwarded(xpAward);
                badge.setAwardedAt(Instant.now());
                seasonBadgeRepository.save(badge);

                // Award XP via progression service
                progressionService.awardXp(
                        student.getId(),
                        xpAward,
                        ActivityType.SEASON_RANKING,
                        student.getCurrentStage(),
                        seasonId,
                        1.0
                );
            }
        }
    }
}
