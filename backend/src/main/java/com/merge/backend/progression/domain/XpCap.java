package com.merge.backend.progression.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-stage, per-activity XP cap.
 *
 * Defines the maximum cumulative XP a student can earn from a given activity type
 * while they hold a particular stage. ProgressionService reads this before every award;
 * when the student has already earned cap_amount, further awards return { awarded:0, capped:true }.
 *
 * A missing row (no cap defined) means the activity is uncapped for that stage.
 */
@Entity
@Table(
    name = "xp_caps",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_xp_caps_stage_activity",
        columnNames = {"stage_name", "activity_type"}
    )
)
@Data
@NoArgsConstructor
public class XpCap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stage this cap applies to — matches stages.name (CADET, ENGINEER, etc.). */
    @Column(name = "stage_name", nullable = false, length = 20)
    private String stageName;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 25)
    private ActivityType activityType;

    /** Maximum cumulative XP a student may earn from this activity while at this stage. */
    @Column(name = "cap_amount", nullable = false)
    private int capAmount;
}
