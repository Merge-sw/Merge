package com.merge.backend.assessment.domain;

import com.merge.backend.identity.domain.Student;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
    name = "drill_submissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_drill_submissions_idempotency_key",
        columnNames = {"idempotency_key"}
    )
)
@Data
@NoArgsConstructor
public class DrillSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drill_id", nullable = false)
    private Drill drill;

    /** Student's solution code — must be present or submission is rejected with 400. */
    @Column(name = "code", nullable = false, columnDefinition = "text")
    private String code;

    /**
     * Student-authored test suite (TDD gate — FACT: T).
     * Must be non-blank; submissions with no tests are blocked at the boundary.
     */
    @Column(name = "test_suite", nullable = false, columnDefinition = "text")
    private String testSuite;

    /**
     * Data layout reasoning (FACT: A — Architecture).
     * Student documents data structure decisions before coding.
     */
    @Column(name = "architecture_data_layout", nullable = false, columnDefinition = "text")
    private String architectureDataLayout;

    /**
     * Algorithm trade-off reasoning (FACT: A — Architecture).
     * Student documents algorithm choices and trade-offs before coding.
     */
    @Column(name = "architecture_algorithm_tradeoffs", nullable = false, columnDefinition = "text")
    private String architectureAlgorithmTradeoffs;

    /**
     * Client-generated UUID preventing duplicate submissions on network retries.
     * UNIQUE constraint enforces exactly-once processing at the database level.
     */
    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DrillSubmissionStatus status = DrillSubmissionStatus.PENDING;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
}
