package com.merge.backend.curriculum.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "concept_resources")
@Data
@NoArgsConstructor
public class ConceptResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private ResourceType type;

    /** Publisher or provider name (e.g. "Fireship", "MDN", "CS50"). */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "url", nullable = false)
    private String url;

    /** Estimated consumption time in minutes; null if unknown. */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /**
     * True when this resource is a recap/revision aid for a concept the student
     * has already encountered — surfaces it as a refresher rather than new material.
     */
    @Column(name = "is_recap", nullable = false)
    private boolean isRecap = false;

    /** XP awarded when the student marks this resource as consumed. */
    @Column(name = "xp_value", nullable = false)
    private int xpValue = 0;

    /** True if the resource is downloadable / usable without internet. */
    @Column(name = "offline_available", nullable = false)
    private boolean offlineAvailable = false;

    /** URL to a text transcript, if available (accessibility / offline reading). */
    @Column(name = "transcript_url")
    private String transcriptUrl;
}
