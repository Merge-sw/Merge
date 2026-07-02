package com.merge.backend.progression.domain;

import com.merge.backend.identity.domain.Student;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
    name = "stage_promotions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_stage_promotions_student_from_stage",
        columnNames = {"student_id", "from_stage"}
    )
)
@Data
@NoArgsConstructor
public class StagePromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "from_stage", nullable = false, length = 20)
    private String fromStage;

    @Column(name = "to_stage", nullable = false, length = 20)
    private String toStage;

    /** student.total_xp at the moment of promotion. */
    @Column(name = "xp_at_promotion", nullable = false)
    private int xpAtPromotion;

    /** Cumulative build pass score (sum of best overallScore per distinct PASSED build) at promotion. */
    @Column(name = "build_score_at_promotion", nullable = false)
    private int buildScoreAtPromotion;

    @Column(name = "promoted_at", nullable = false)
    private Instant promotedAt;
}
