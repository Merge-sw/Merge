package com.merge.backend.engagement.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merge.backend.engagement.dto.SeasonLockPayload;
import com.merge.backend.engagement.service.SeasonService;
import com.merge.backend.infrastructure.queue.JobHandler;
import com.merge.backend.infrastructure.queue.JobPayload;
import com.merge.backend.infrastructure.queue.JobType;
import org.springframework.stereotype.Component;

@Component
public class SeasonLockHandler implements JobHandler {

    private final SeasonService seasonService;
    private final ObjectMapper objectMapper;

    public SeasonLockHandler(SeasonService seasonService, ObjectMapper objectMapper) {
        this.seasonService = seasonService;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType jobType() {
        return JobType.SEASON_LOCK;
    }

    @Override
    public void handle(JobPayload payload) throws Exception {
        SeasonLockPayload data = objectMapper.readValue(
                payload.getPayloadJson(), SeasonLockPayload.class);
        seasonService.lockSeasonAndAwardBadges(data.getSeasonId());
    }
}
