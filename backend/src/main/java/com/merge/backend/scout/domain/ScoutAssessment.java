package com.merge.backend.scout.domain;

import com.merge.backend.identity.domain.Student;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "scout_assessments")
@Data
@NoArgsConstructor
public class ScoutAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layer1_responses", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> layer1Responses;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layer2_results", columnDefinition = "jsonb")
    private Map<String, String> layer2Results;

    @Column(name = "layer2_submitted_at")
    private Instant layer2SubmittedAt;

    @PrePersist
    void onPersist() {
        submittedAt = Instant.now();
    }
}
