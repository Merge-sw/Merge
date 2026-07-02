package com.merge.backend.engagement.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merge.backend.engagement.dto.SeasonLockPayload;
import com.merge.backend.engagement.service.SeasonService;
import com.merge.backend.infrastructure.queue.JobPayload;
import com.merge.backend.infrastructure.queue.JobType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class SeasonLockHandlerTest {

    private SeasonLockHandler handler;

    @Mock
    private SeasonService seasonService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new SeasonLockHandler(seasonService, objectMapper);
    }

    @Test
    public void testJobType() {
        assertEquals(JobType.SEASON_LOCK, handler.jobType());
    }

    @Test
    public void testHandle() throws Exception {
        SeasonLockPayload payload = new SeasonLockPayload();
        payload.setSeasonId(42L);
        String json = objectMapper.writeValueAsString(payload);

        JobPayload jobPayload = mock(JobPayload.class);
        when(jobPayload.getPayloadJson()).thenReturn(json);

        handler.handle(jobPayload);

        verify(seasonService, times(1)).lockSeasonAndAwardBadges(42L);
    }
}
