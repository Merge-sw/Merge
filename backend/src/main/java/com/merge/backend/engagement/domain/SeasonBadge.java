package com.merge.backend.engagement.domain;

import com.merge.backend.identity.domain.Student;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "season_badges")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeasonBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "badge_type", nullable = false)
    private String badgeType; // GOLD, SILVER

    @Column(nullable = false)
    private int rank;

    @Column(nullable = false)
    private double percentile;

    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded;

    @Column(name = "awarded_at", nullable = false)
    private Instant awardedAt = Instant.now();
}
