package com.merge.backend.engagement.repository;

import com.merge.backend.engagement.domain.AudioRecord;
import com.merge.backend.engagement.domain.AudioType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AudioRecordRepository extends JpaRepository<AudioRecord, Long> {
    Optional<AudioRecord> findFirstByStudentIdAndConceptIdAndAudioTypeOrderByGeneratedAtDesc(
            Long studentId, Long conceptId, AudioType audioType);
}
