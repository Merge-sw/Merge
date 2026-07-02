package com.merge.backend.engagement.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merge.backend.engagement.domain.AudioType;
import com.merge.backend.engagement.dto.AudioGenerationPayload;
import com.merge.backend.engagement.service.AudioGenerationService;
import com.merge.backend.infrastructure.queue.JobPayload;
import com.merge.backend.infrastructure.queue.JobType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class AudioGenerationHandlerTest {

    private AudioGenerationHandler handler;

    @Mock
    private AudioGenerationService audioGenerationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new AudioGenerationHandler(audioGenerationService, objectMapper);
    }

    @Test
    public void testJobType() {
        assertEquals(JobType.AUDIO_GENERATION, handler.jobType());
    }

    @Test
    public void testHandle() throws Exception {
        AudioGenerationPayload payload = new AudioGenerationPayload();
        payload.setStudentId(101L);
        payload.setConceptId(202L);
        payload.setAudioType("PRIMER");
        String json = objectMapper.writeValueAsString(payload);

        JobPayload jobPayload = mock(JobPayload.class);
        when(jobPayload.getPayloadJson()).thenReturn(json);

        handler.handle(jobPayload);

        verify(audioGenerationService, times(1)).generateAudio(101L, 202L, AudioType.PRIMER);
    }
}
