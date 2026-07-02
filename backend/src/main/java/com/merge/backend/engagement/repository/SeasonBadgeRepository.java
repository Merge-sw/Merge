package com.merge.backend.engagement.repository;

import com.merge.backend.engagement.domain.SeasonBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeasonBadgeRepository extends JpaRepository<SeasonBadge, Long> {
    List<SeasonBadge> findByStudentId(Long studentId);
}
