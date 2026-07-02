package com.merge.backend.engagement.domain;

import com.merge.backend.curriculum.domain.Concept;
import com.merge.backend.identity.domain.Student;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audio_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AudioRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    @Enumerated(EnumType.STRING)
    @Column(name = "audio_type", nullable = false)
    private AudioType audioType;

    @Column(name = "script_text", nullable = false, columnDefinition = "text")
    private String scriptText;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    @Column(nullable = false)
    private boolean listened = false;

    @Column(name = "listened_at")
    private Instant listenedAt;
}
