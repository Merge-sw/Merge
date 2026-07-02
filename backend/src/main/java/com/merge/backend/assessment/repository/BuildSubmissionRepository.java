package com.merge.backend.assessment.repository;

import com.merge.backend.assessment.domain.BuildSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuildSubmissionRepository extends JpaRepository<BuildSubmission, Long> {

    Optional<BuildSubmission> findByIdempotencyKey(String idempotencyKey);

    /** Gate 3 (AI-07 clean-code) scores for all prior attempts on builds in the given stage. */
    @Query("SELECT bs.overallScore FROM BuildSubmission bs " +
           "WHERE bs.student.id = :studentId " +
           "AND bs.build.stageName = :stageName " +
           "AND bs.overallScore IS NOT NULL " +
           "ORDER BY bs.submittedAt ASC")
    java.util.List<Integer> findCleanCodeScoresByStudentAndStage(
            @Param("studentId") Long studentId,
            @Param("stageName") String stageName);

    /** Counts previous submissions to determine the next attempt_number. */
    int countByStudentIdAndBuildId(Long studentId, Long buildId);

    /**
     * Returns the student's cumulative build pass score for a given stage:
     * SUM of the best (MAX) overallScore per distinct build where the student
     * has at least one PASSED submission. Used for promotion-status gate checks.
     */
    @Query(value = """
            SELECT COALESCE(SUM(best.score), 0)
            FROM (
                SELECT MAX(bs.overall_score) AS score
                FROM build_submissions bs
                INNER JOIN builds b ON b.id = bs.build_id
                WHERE bs.student_id   = :studentId
                  AND b.stage_name    = :stageName
                  AND bs.overall_status = 'PASSED'
                  AND bs.overall_score IS NOT NULL
                GROUP BY bs.build_id
            ) best
            """, nativeQuery = true)
    int sumBestPassScoreByStudentAndStage(@Param("studentId") Long studentId,
                                          @Param("stageName") String stageName);
}
