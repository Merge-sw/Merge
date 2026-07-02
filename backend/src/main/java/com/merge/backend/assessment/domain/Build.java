package com.merge.backend.assessment.domain;

import com.merge.backend.identity.domain.Student;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

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
     * AI-03-generated personalised Product Requirements Document.
     * Derived from David's specific Drill performance history — not a generic template.
     * All four AI-generated fields below are null until BUILD_PRD_GENERATION job completes.
     * The GET endpoint returns 202 { generating: true } while any are null.
     */
    @Column(name = "prd", columnDefinition = "text")
    private String prd;

    /**
     * Functional requirements extracted from the AI-03 PRD, stored as an ordered list.
     * e.g. ["Implement a rate-limited API client", "Support exponential back-off on 429"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requirements", columnDefinition = "jsonb")
    private List<String> requirements;

    /**
     * Non-functional / technical constraints the student must respect.
     * e.g. ["No third-party HTTP libraries", "Must handle 10k req/s without external state"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "constraints", columnDefinition = "jsonb")
    private List<String> constraints;

    /**
     * SFIA competency tags this build challenge is designed to assess.
     * e.g. ["PROBLEM_SOLVING", "SOFTWARE_DESIGN", "TESTING_AND_DEBUGGING"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sfia_competencies", columnDefinition = "jsonb")
    private List<String> sfiaCompetencies;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
