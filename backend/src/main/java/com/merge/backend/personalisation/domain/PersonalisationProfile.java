package com.merge.backend.personalisation.domain;

import com.merge.backend.identity.domain.Student;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "personalisation_profiles")
@Data
@NoArgsConstructor
public class PersonalisationProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weak_concepts", columnDefinition = "jsonb")
    private List<String> weakConcepts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strength_concepts", columnDefinition = "jsonb")
    private List<String> strengthConcepts;

    @Enumerated(EnumType.STRING)
    @Column(name = "scaffolding_level")
    private ScaffoldingLevel scaffoldingLevel;

    /** Exponential moving average of session duration in milliseconds. */
    @Column(name = "avg_session_duration")
    private Long avgSessionDuration;

    /** Cumulative hint usage per concept across all sessions: concept → total hint count. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hint_usage_pattern", columnDefinition = "jsonb")
    private Map<String, Integer> hintUsagePattern;

    /** AI-derived coding style observations: pattern name → frequency or detail. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "coding_style_patterns", columnDefinition = "jsonb")
    private Map<String, Object> codingStylePatterns;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
