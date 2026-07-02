package com.merge.backend.assessment.domain;

import com.merge.backend.identity.domain.Student;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
    name = "build_submissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_build_submissions_idempotency_key",
        columnNames = {"idempotency_key"}
    )
)
@Data
@NoArgsConstructor
public class BuildSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "build_id", nullable = false)
    private Build build;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "code", nullable = false, columnDefinition = "text")
    private String code;

    @Column(name = "test_suite", nullable = false, columnDefinition = "text")
    private String testSuite;

    @Column(name = "architecture_document", nullable = false, columnDefinition = "text")
    private String architectureDocument;

    /**
     * Client-generated UUID for exactly-once submission semantics.
     * UNIQUE constraint at the DB level — same key → 409 with the original submission.
     */
    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    /**
     * 1-based count of how many times this student has attempted this build.
     * Drives XP decay: 1st=100%, 2nd=75%, 3rd=50%, 4th+=25%.
     */
    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false, length = 10)
    private BuildSubmissionStatus overallStatus = BuildSubmissionStatus.PENDING;

    /**
     * XP that will be awarded if all five gates pass.
     * Pre-calculated at submission time using the attempt-based decay formula
     * so the student knows the potential reward before all gates complete.
     */
    @Column(name = "pending_xp", nullable = false)
    private int pendingXp;

    /**
     * Whether Gate 1 (JUDGE0 against hidden test suite) passed.
     * Gate 1 must be true for any XP to be awarded — it is a hard blocker
     * regardless of other gates' outcomes.
     * Null until Gate 1 has been evaluated for this submission.
     */
    @Column(name = "gate1_passed")
    private Boolean gate1Passed;

    /**
     * Whether Gate 2 (student's own test suite executed via Judge0) passed.
     * Requires: all submitted tests pass AND static quality checks pass
     * (non-empty, non-trivial, Cadet minimum met).
     * Null until Gate 2 has been evaluated for this submission.
     */
    @Column(name = "gate2_passed")
    private Boolean gate2Passed;

    /**
     * Whether Gate 3 (AI-07 CleanCodeReviewer) passed.
     * Pass condition: overallScore >= Stage.cleanCodeMinScore.
     * Null until Gate 3 has been evaluated for this submission.
     */
    @Column(name = "gate3_passed")
    private Boolean gate3Passed;

    /**
     * Numeric clean-code score returned by AI-07 (0–100).
     * Recorded for the student to see their exact score and the gap to the threshold.
     * Null until Gate 3 has been evaluated for this submission.
     */
    @Column(name = "overall_score")
    private Integer overallScore;

    /** Populated once all gates pass and XP has been awarded via ProgressionService. */
    @Column(name = "xp_awarded")
    private Integer xpAwarded;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
}
