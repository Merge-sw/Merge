package com.merge.backend.assessment.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merge.backend.assessment.dto.GithubCommitJobPayload;
import com.merge.backend.assessment.service.GithubCommitService;
import com.merge.backend.infrastructure.queue.JobPayload;
import com.merge.backend.infrastructure.queue.JobType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class GithubCommitHandlerTest {

    private GithubCommitHandler handler;

    @Mock
    private GithubCommitService githubCommitService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new GithubCommitHandler(githubCommitService, objectMapper);
    }

    @Test
    public void testJobType() {
        assertEquals(JobType.GITHUB_COMMIT, handler.jobType());
    }

    @Test
    public void testHandle() throws Exception {
        GithubCommitJobPayload data = new GithubCommitJobPayload(10L, 1L, 5L);
        String json = objectMapper.writeValueAsString(data);

        JobPayload jobPayload = mock(JobPayload.class);
        when(jobPayload.getPayloadJson()).thenReturn(json);

        handler.handle(jobPayload);

        verify(githubCommitService, times(1)).processCommitJob(data);
    }
}
