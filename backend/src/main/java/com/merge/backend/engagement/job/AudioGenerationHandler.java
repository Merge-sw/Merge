package com.merge.backend.engagement.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merge.backend.engagement.domain.AudioType;
import com.merge.backend.engagement.dto.AudioGenerationPayload;
import com.merge.backend.engagement.service.AudioGenerationService;
import com.merge.backend.infrastructure.queue.JobHandler;
import com.merge.backend.infrastructure.queue.JobPayload;
import com.merge.backend.infrastructure.queue.JobType;
import org.springframework.stereotype.Component;

@Component
public class AudioGenerationHandler implements JobHandler {

    private final AudioGenerationService audioGenerationService;
    private final ObjectMapper objectMapper;

    public AudioGenerationHandler(AudioGenerationService audioGenerationService, ObjectMapper objectMapper) {
        this.audioGenerationService = audioGenerationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType jobType() {
        return JobType.AUDIO_GENERATION;
    }

    @Override
    public void handle(JobPayload payload) throws Exception {
        AudioGenerationPayload data = objectMapper.readValue(
                payload.getPayloadJson(), AudioGenerationPayload.class);
        AudioType audioType = AudioType.valueOf(data.getAudioType());
        audioGenerationService.generateAudio(data.getStudentId(), data.getConceptId(), audioType);
    }
}
