package com.merge.backend.assessment.domain;

import com.merge.backend.identity.domain.Student;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
    name = "code_reading_submissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_code_reading_student_drill",
        columnNames = {"student_id", "drill_id"}
    )
)
@Data
@NoArgsConstructor
public class CodeReadingSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Always Drill 2 — code reading is only gated on Drill 2.
     * The UNIQUE constraint prevents duplicate submissions per student+drill.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drill_id", nullable = false)
    private Drill drill;

    /** Student's free-text analysis of the unfamiliar codebase. Not graded — only presence is checked. */
    @Column(name = "response_text", nullable = false, columnDefinition = "text")
    private String responseText;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
}
