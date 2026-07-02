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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SeasonServiceTest {

    private SeasonService seasonService;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private WeeklyMomentumRepository weeklyMomentumRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SeasonBadgeRepository seasonBadgeRepository;

    @Mock
    private ProgressionService progressionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        seasonService = new SeasonServiceImpl(
                seasonRepository,
                weeklyMomentumRepository,
                studentRepository,
                seasonBadgeRepository,
                progressionService
        );
    }

    @Test
    public void testCreateSeason_ActiveDeactivatesOther() {
        Season existingActive = new Season(1L, "Fall 2025", Instant.now(), Instant.now(), true, false);
        when(seasonRepository.findByActive(true)).thenReturn(Optional.of(existingActive));

        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> {
            Season s = invocation.getArgument(0);
            if (s.getId() == null) {
                s.setId(2L);
            }
            return s;
        });

        Season result = seasonService.createSeason("Spring 2026", Instant.now(), Instant.now(), true);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertTrue(result.isActive());
        assertFalse(existingActive.isActive());

        verify(seasonRepository, times(1)).save(existingActive);
        verify(seasonRepository, times(2)).save(any(Season.class));
    }

    @Test
    public void testCloseSeason_LocksWeeklyMomentum() {
        Instant now = Instant.now();
        Season activeSeason = new Season(1L, "Fall 2025", now, now.plusSeconds(3600), true, false);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(activeSeason));
        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Season result = seasonService.closeSeason(1L);

        assertNotNull(result);
        assertFalse(result.isActive());
        assertTrue(result.isClosed());

        verify(seasonRepository, times(1)).save(activeSeason);
        verify(weeklyMomentumRepository, times(1)).lockAllForDateRange(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    public void testLockSeasonAndAwardBadges() {
        Instant now = Instant.now();
        Season activeSeason = new Season(1L, "Fall 2025", now, now.plusSeconds(3600), true, false);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(activeSeason));
        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Create 10 mock students with different XP to verify top 10% and top 25% thresholds
        List<Student> students = List.of(
                new Student(1L, "Student 1", "s1@test.com", "111", "s1@u.edu", "hash", "CADET", 1000, null, null, null),
                new Student(2L, "Student 2", "s2@test.com", "222", "s2@u.edu", "hash", "CADET", 900, null, null, null),
                new Student(3L, "Student 3", "s3@test.com", "333", "s3@u.edu", "hash", "CADET", 800, null, null, null),
                new Student(4L, "Student 4", "s4@test.com", "444", "s4@u.edu", "hash", "CADET", 700, null, null, null),
                new Student(5L, "Student 5", "s5@test.com", "555", "s5@u.edu", "hash", "CADET", 600, null, null, null),
                new Student(6L, "Student 6", "s6@test.com", "666", "s6@u.edu", "hash", "CADET", 500, null, null, null),
                new Student(7L, "Student 7", "s7@test.com", "777", "s7@u.edu", "hash", "CADET", 400, null, null, null),
                new Student(8L, "Student 8", "s8@test.com", "888", "s8@u.edu", "hash", "CADET", 300, null, null, null),
                new Student(9L, "Student 9", "s9@test.com", "999", "s9@u.edu", "hash", "CADET", 200, null, null, null),
                new Student(10L, "Student 10", "s10@test.com", "000", "s10@u.edu", "hash", "CADET", 100, null, null, null)
        );
        when(studentRepository.findAll()).thenReturn(students);

        seasonService.lockSeasonAndAwardBadges(1L);

        // Verify season was closed
        assertFalse(activeSeason.isActive());
        assertTrue(activeSeason.isClosed());

        // Verify weekly momentum records locked
        verify(weeklyMomentumRepository, times(1)).lockAllForDateRange(any(LocalDate.class), any(LocalDate.class));

        // 10 students:
        // Rank 1 (10%): GOLD badge + 200 XP
        // Rank 2 (20%): SILVER badge + 100 XP
        // Rank 3-10: no badge

        // Verify GOLD badge saved for Student 1
        verify(seasonBadgeRepository, times(1)).save(argThat(badge -> 
                badge.getStudent().getId().equals(1L) && 
                badge.getBadgeType().equals("GOLD") && 
                badge.getRank() == 1
        ));

        // Verify SILVER badge saved for Student 2
        verify(seasonBadgeRepository, times(1)).save(argThat(badge -> 
                badge.getStudent().getId().equals(2L) && 
                badge.getBadgeType().equals("SILVER") && 
                badge.getRank() == 2
        ));

        // Verify XP awarded
        verify(progressionService, times(1)).awardXp(
                eq(1L), eq(200), eq(ActivityType.SEASON_RANKING), eq("CADET"), eq(1L), eq(1.0)
        );
        verify(progressionService, times(1)).awardXp(
                eq(2L), eq(100), eq(ActivityType.SEASON_RANKING), eq("CADET"), eq(1L), eq(1.0)
        );
        
        // Total badge saves should be 2
        verify(seasonBadgeRepository, times(2)).save(any(SeasonBadge.class));
    }
}
