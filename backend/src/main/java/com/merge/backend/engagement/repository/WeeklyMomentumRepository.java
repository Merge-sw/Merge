package com.merge.backend.engagement.repository;

import com.merge.backend.engagement.domain.WeeklyMomentum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyMomentumRepository extends JpaRepository<WeeklyMomentum, Long> {

    Optional<WeeklyMomentum> findByStudentIdAndWeekStart(Long studentId, LocalDate weekStart);

    /**
     * Returns the most recent WeeklyMomentum record per student (one row per student who has
     * any record). Students with no record are absent from the result — callers default them to OFFLINE.
     */
    @Query("SELECT wm FROM WeeklyMomentum wm WHERE wm.weekStart = (" +
           "  SELECT MAX(wm2.weekStart) FROM WeeklyMomentum wm2 WHERE wm2.student.id = wm.student.id)")
    List<WeeklyMomentum> findLatestPerStudent();
}
