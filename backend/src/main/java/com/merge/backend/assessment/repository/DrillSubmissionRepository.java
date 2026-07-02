package com.merge.backend.assessment.repository;

import com.merge.backend.assessment.domain.DrillSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface DrillSubmissionRepository extends JpaRepository<DrillSubmission, Long> {

    Optional<DrillSubmission> findByIdempotencyKey(String idempotencyKey);

    /** Counts previous submissions to determine the next attempt_number. */
    int countByStudentIdAndDrillId(Long studentId, Long drillId);

    /** Half-open interval [from, to) — total submissions in a weekly window, for pass-rate calculation. */
    @Query("SELECT COUNT(ds) FROM DrillSubmission ds " +
           "WHERE ds.student.id = :studentId " +
           "AND ds.submittedAt >= :from AND ds.submittedAt < :to")
    int countAttemptsBetween(@Param("studentId") Long studentId,
                             @Param("from") Instant from,
                             @Param("to") Instant to);
}
