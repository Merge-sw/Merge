package com.merge.backend.infrastructure.queue;

/**
 * Implemented by each module that processes a specific JobType.
 * Register as a Spring bean; the dispatcher discovers all implementations automatically.
 */
public interface JobHandler {
    JobType jobType();
    void handle(JobPayload payload) throws Exception;
}
