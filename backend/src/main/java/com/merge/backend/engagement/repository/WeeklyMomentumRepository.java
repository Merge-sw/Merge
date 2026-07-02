package com.merge.backend.engagement.repository;

import com.merge.backend.engagement.domain.WeeklyMomentum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WeeklyMomentumRepository extends JpaRepository<WeeklyMomentum, Long> {
    Optional<WeeklyMomentum> findByStudentIdAndWeekStart(Long studentId, LocalDate weekStart);

    @Modifying
    @Query("UPDATE WeeklyMomentum w SET w.locked = true WHERE w.weekStart >= :startDate AND w.weekStart <= :endDate")
    void lockAllForDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
