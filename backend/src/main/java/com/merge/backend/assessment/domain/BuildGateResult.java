package com.merge.backend.assessment.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "build_gate_results")
@Data
@NoArgsConstructor
public class BuildGateResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "build_submission_id", nullable = false)
    private BuildSubmission buildSubmission;

    @Enumerated(EnumType.STRING)
    @Column(name = "gate", nullable = false, length = 30)
    private BuildGate gate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private BuildGateStatus status = BuildGateStatus.PENDING;

    /** AI feedback text returned to the student. Null for PENDING results. */
    @Column(name = "feedback", columnDefinition = "text")
    private String feedback;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;
}
