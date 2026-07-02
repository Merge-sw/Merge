package com.merge.backend.assessment.repository;

import com.merge.backend.assessment.domain.DrillSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DrillSubmissionRepository extends JpaRepository<DrillSubmission, Long> {

    Optional<DrillSubmission> findByIdempotencyKey(String idempotencyKey);

    /** Counts previous submissions to determine the next attempt_number. */
    int countByStudentIdAndDrillId(Long studentId, Long drillId);
}
