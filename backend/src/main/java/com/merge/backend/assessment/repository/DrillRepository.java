package com.merge.backend.assessment.repository;

import com.merge.backend.assessment.domain.Drill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrillRepository extends JpaRepository<Drill, Long> {

    List<Drill> findByStudentIdAndConceptIdOrderByDrillNumberAsc(Long studentId, Long conceptId);

    Optional<Drill> findByStudentIdAndConceptIdAndDrillNumber(
            Long studentId, Long conceptId, int drillNumber);
}
