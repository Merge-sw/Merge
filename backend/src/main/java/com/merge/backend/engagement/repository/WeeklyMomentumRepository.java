package com.merge.backend.engagement.repository;

import com.merge.backend.engagement.domain.WeeklyMomentum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WeeklyMomentumRepository extends JpaRepository<WeeklyMomentum, Long> {

    Optional<WeeklyMomentum> findByStudentIdAndWeekStart(Long studentId, LocalDate weekStart);
}
