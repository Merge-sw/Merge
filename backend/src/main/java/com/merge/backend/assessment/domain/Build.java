package com.merge.backend.assessment.domain;

import com.merge.backend.identity.domain.Student;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
    name = "builds",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_builds_student_stage",
        columnNames = {"student_id", "stage_name"}
    )
)
@Data
@NoArgsConstructor
public class Build {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** Stage this build belongs to — SCOUT, CADET, ENGINEER, ARCHITECT, PRINCIPAL. */
    @Column(name = "stage_name", nullable = false, length = 20)
    private String stageName;

    /** Set to true once both conditions are simultaneously satisfied. */
    @Column(name = "is_unlocked", nullable = false)
    private boolean isUnlocked = false;

    /** Server-recorded instant when unlock conditions were first met. */
    @Column(name = "unlocked_at")
    private Instant unlockedAt;

    /**
     * AI-03-generated Product Requirements Document for this student's build challenge.
     * Null until the BUILD_PRD_GENERATION job completes.
     * The GET endpoint returns 202 while this is null, 200 once populated.
     */
    @Column(name = "prd", columnDefinition = "text")
    private String prd;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
