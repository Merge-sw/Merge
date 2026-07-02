package com.merge.backend.assessment.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merge.backend.assessment.dto.GithubCommitJobPayload;
import com.merge.backend.assessment.service.GithubCommitService;
import com.merge.backend.infrastructure.queue.JobHandler;
import com.merge.backend.infrastructure.queue.JobPayload;
import com.merge.backend.infrastructure.queue.JobType;
import org.springframework.stereotype.Component;

@Component
public class GithubCommitHandler implements JobHandler {

    private final GithubCommitService githubCommitService;
    private final ObjectMapper objectMapper;

    public GithubCommitHandler(GithubCommitService githubCommitService, ObjectMapper objectMapper) {
        this.githubCommitService = githubCommitService;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType jobType() {
        return JobType.GITHUB_COMMIT;
    }

    @Override
    public void handle(JobPayload payload) throws Exception {
        GithubCommitJobPayload data = objectMapper.readValue(
                payload.getPayloadJson(), GithubCommitJobPayload.class);
        githubCommitService.processCommitJob(data);
    }
}
