package com.merge.backend.engagement.service;

import com.merge.backend.engagement.domain.AudioRecord;
import com.merge.backend.engagement.domain.AudioType;

public interface AudioGenerationService {
    AudioRecord generateAudio(Long studentId, Long conceptId, AudioType audioType);
}
