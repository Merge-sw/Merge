package com.merge.backend.assessment.repository;

import com.merge.backend.assessment.domain.BuildSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuildSubmissionRepository extends JpaRepository<BuildSubmission, Long> {

    Optional<BuildSubmission> findByIdempotencyKey(String idempotencyKey);

    /** Counts previous submissions to determine the next attempt_number. */
    int countByStudentIdAndBuildId(Long studentId, Long buildId);
}
