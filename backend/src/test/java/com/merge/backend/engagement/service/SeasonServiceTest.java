package com.merge.backend.engagement.service;

import com.merge.backend.engagement.domain.Season;
import com.merge.backend.engagement.repository.SeasonRepository;
import com.merge.backend.engagement.repository.WeeklyMomentumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.LocalDate;
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

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        seasonService = new SeasonServiceImpl(seasonRepository, weeklyMomentumRepository);
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
    public void testCloseSeason_AlreadyClosedThrowsException() {
        Season closedSeason = new Season(1L, "Fall 2025", Instant.now(), Instant.now(), false, true);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(closedSeason));

        assertThrows(IllegalStateException.class, () -> seasonService.closeSeason(1L));
        verify(seasonRepository, never()).save(any(Season.class));
        verify(weeklyMomentumRepository, never()).lockAllForDateRange(any(LocalDate.class), any(LocalDate.class));
    }
}
