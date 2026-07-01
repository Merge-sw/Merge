package com.merge.backend.infrastructure.worker;

import com.merge.backend.infrastructure.queue.JobPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Fallback FinalFailureHandler active only when INF-02 (DLQ + Sentry) is not present.
 * Logs the failure at ERROR level and takes no further action.
 */
@Component
@ConditionalOnMissingBean(name = "jobErrorHandler")
public class LogOnlyFinalFailureHandler implements FinalFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(LogOnlyFinalFailureHandler.class);

    @Override
    public void onFinalFailure(JobPayload job, Throwable cause) {
        log.error("[FinalFailure] type={} jobId={} attempts={} error={}",
                job.getJobType(), job.getJobId(), job.getAttemptCount(), cause.getMessage(), cause);
    }
}
