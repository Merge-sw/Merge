package com.merge.backend.personalisation.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merge.backend.infrastructure.queue.JobHandler;
import com.merge.backend.infrastructure.queue.JobPayload;
import com.merge.backend.infrastructure.queue.JobType;
import com.merge.backend.personalisation.dto.SessionAnalysisPayload;
import com.merge.backend.personalisation.service.PersonalisationUpdateService;
import org.springframework.stereotype.Component;

/**
 * Consumes PERSONALISATION_UPDATE jobs enqueued by EN-02 at session end.
 * Non-critical: dispatcher retries once on failure, then skips (DLQ via INF-02).
 */
@Component
public class PersonalisationUpdateHandler implements JobHandler {

    private final PersonalisationUpdateService updateService;
    private final ObjectMapper objectMapper;

    public PersonalisationUpdateHandler(PersonalisationUpdateService updateService,
                                        ObjectMapper objectMapper) {
        this.updateService = updateService;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType jobType() {
        return JobType.PERSONALISATION_UPDATE;
    }

    @Override
    public void handle(JobPayload payload) throws Exception {
        SessionAnalysisPayload sessionPayload = objectMapper.readValue(
                payload.getPayloadJson(), SessionAnalysisPayload.class);
        updateService.processSessionUpdate(sessionPayload);
    }
}
