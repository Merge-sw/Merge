package com.merge.backend.infrastructure.error;

import com.merge.backend.infrastructure.queue.DeadLetterQueueService;
import com.merge.backend.infrastructure.queue.JobCriticality;
import com.merge.backend.infrastructure.queue.JobPayload;
import com.merge.backend.infrastructure.worker.FinalFailureHandler;
import io.sentry.SentryLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Central handler for jobs that have exhausted all retry attempts.
 *
 * CRITICAL          → DLQ + Sentry FATAL + ERROR log (data preserved, alert fires via Sentry rule)
 * NON_CRITICAL      → DLQ + Sentry WARNING + WARN log (safe to skip)
 * SCHEDULED         → ERROR log only (job recurs on schedule; no DLQ, no Sentry)
 */
@Component
public class JobErrorHandler implements FinalFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(JobErrorHandler.class);

    private final DeadLetterQueueService dlq;
    private final SentryService sentry;

    public JobErrorHandler(DeadLetterQueueService dlq, SentryService sentry) {
        this.dlq = dlq;
        this.sentry = sentry;
    }

    public void onFinalFailure(JobPayload job, Throwable cause) {
        JobCriticality criticality = job.getJobType().criticality;

        switch (criticality) {
            case CRITICAL -> handleCritical(job, cause);
            case NON_CRITICAL -> handleNonCritical(job, cause);
            case SCHEDULED -> handleScheduled(job, cause);
        }
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private void handleCritical(JobPayload job, Throwable cause) {
        log.error("[CRITICAL_JOB_FAILURE] type={} jobId={} attempts={} error={}",
                job.getJobType(), job.getJobId(), job.getAttemptCount(), cause.getMessage(), cause);

        dlq.park(job);
        sentry.captureJobFailure(job, cause, SentryLevel.FATAL);

        // Structured alert marker — configure a Sentry alert rule on tag job.criticality=CRITICAL
        log.error("[ALERT] Critical job {} parked in DLQ. Manual intervention required. jobId={}",
                job.getJobType(), job.getJobId());
    }

    private void handleNonCritical(JobPayload job, Throwable cause) {
        log.warn("[NON_CRITICAL_JOB_FAILURE] type={} jobId={} attempts={} — skipping. error={}",
                job.getJobType(), job.getJobId(), job.getAttemptCount(), cause.getMessage());

        dlq.park(job);
        sentry.captureJobFailure(job, cause, SentryLevel.WARNING);
    }

    private void handleScheduled(JobPayload job, Throwable cause) {
        log.error("[SCHEDULED_JOB_ERROR] type={} jobId={} error={}",
                job.getJobType(), job.getJobId(), cause.getMessage(), cause);
        // No DLQ, no Sentry — scheduled jobs retry naturally on their next run
    }
}
