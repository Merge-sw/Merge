package com.merge.backend.infrastructure.worker;

import com.merge.backend.infrastructure.queue.JobPayload;

/**
 * Called by JobDispatcher when a job has exhausted all retry attempts.
 * INF-02 provides the production implementation (DLQ + Sentry).
 * A no-op default bean is registered here so INF-01 compiles standalone.
 */
public interface FinalFailureHandler {
    void onFinalFailure(JobPayload job, Throwable cause);
}
