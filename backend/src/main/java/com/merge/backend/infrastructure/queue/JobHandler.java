package com.merge.backend.infrastructure.queue;

/**
 * Implemented by each module that processes a specific JobType.
 * The dispatcher looks up the matching handler at runtime.
 */
public interface JobHandler {
    JobType jobType();
    void handle(JobPayload payload) throws Exception;
}
