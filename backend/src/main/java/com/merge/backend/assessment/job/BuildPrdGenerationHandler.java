package com.merge.backend.assessment.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merge.backend.assessment.dto.BuildPrdPayload;
import com.merge.backend.assessment.service.BuildPrdGenerationService;
import com.merge.backend.infrastructure.queue.JobHandler;
import com.merge.backend.infrastructure.queue.JobPayload;
import com.merge.backend.infrastructure.queue.JobType;
import org.springframework.stereotype.Component;

@Component
public class BuildPrdGenerationHandler implements JobHandler {

    private final BuildPrdGenerationService buildPrdGenerationService;
    private final ObjectMapper objectMapper;

    public BuildPrdGenerationHandler(BuildPrdGenerationService buildPrdGenerationService,
                                     ObjectMapper objectMapper) {
        this.buildPrdGenerationService = buildPrdGenerationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType jobType() {
        return JobType.BUILD_PRD_GENERATION;
    }

    @Override
    public void handle(JobPayload payload) throws Exception {
        BuildPrdPayload data = objectMapper.readValue(
                payload.getPayloadJson(), BuildPrdPayload.class);
        buildPrdGenerationService.generate(data.buildId(), data.studentId(), data.stageName());
    }
}
