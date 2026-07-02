package com.merge.backend.curriculum.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "syntax_exercises")
@Data
@NoArgsConstructor
public class SyntaxExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    /** Intentionally broken code the student must fix. */
    @Column(name = "broken_code", nullable = false, columnDefinition = "text")
    private String brokenCode;

    /** What the student is asked to identify and correct. */
    @Column(name = "instructions", nullable = false, columnDefinition = "text")
    private String instructions;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    /**
     * Canonical correct solution used for submission comparison.
     * Comparison is whitespace-normalised so minor formatting differences pass.
     */
    @Column(name = "solution_code", nullable = false, columnDefinition = "text")
    private String solutionCode;
}
