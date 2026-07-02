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

    /** Populated once all gates pass and XP has been awarded via ProgressionService. */
    @Column(name = "xp_awarded")
    private Integer xpAwarded;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
}
