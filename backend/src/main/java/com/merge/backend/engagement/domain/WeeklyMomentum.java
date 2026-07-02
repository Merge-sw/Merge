package com.merge.backend.engagement.domain;

import com.merge.backend.identity.domain.Student;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "weekly_momentum")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyMomentum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MomentumState state;

    @Column(name = "session_count", nullable = false)
    private int sessionCount;

    @Column(name = "drill_pass_rate")
    private Double drillPassRate;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt = Instant.now();
}
