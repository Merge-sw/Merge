package com.merge.backend.assessment.repository;

import com.merge.backend.assessment.domain.DrillCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DrillCompletionRepository extends JpaRepository<DrillCompletion, Long> {

    /**
     * Counts how many distinct drill numbers for a given concept this student has
     * passed comprehension on. Returns 2 when both Drill 1 and Drill 2 have passed.
     */
    @Query("SELECT COUNT(DISTINCT dc.drill.drillNumber) FROM DrillCompletion dc " +
           "WHERE dc.student.id = :studentId " +
           "AND dc.drill.concept.id = :conceptId " +
           "AND dc.comprehensionPassed = true")
    int countPassedComprehensionDrillsForConcept(
            @Param("studentId") Long studentId,
            @Param("conceptId") Long conceptId);

    /** Used to gate Drill 2: returns true if student has passed comprehension on a specific drill. */
    boolean existsByStudentIdAndDrillIdAndComprehensionPassedTrue(Long studentId, Long drillId);

    /** Used before creating a new completion record to avoid duplicates. */
    Optional<DrillCompletion> findByStudentIdAndDrillId(Long studentId, Long drillId);

    /**
     * Counts distinct drills (by drill_id) that this student has passed comprehension on
     * across all concepts of a given stage. Used to verify all drills are done before
     * unlocking the build. Native query avoids JPQL CONCAT limitations.
     */
    @Query(value = "SELECT COUNT(DISTINCT dc.drill_id) " +
                   "FROM drill_completions dc " +
                   "JOIN drills d ON dc.drill_id = d.id " +
                   "JOIN concepts c ON d.concept_id = c.id " +
                   "WHERE dc.student_id = :studentId " +
                   "AND c.stage_name = :stageName " +
                   "AND dc.comprehension_passed = true",
           nativeQuery = true)
    long countPassedDrillsForStage(@Param("studentId") Long studentId,
                                   @Param("stageName") String stageName);
}
