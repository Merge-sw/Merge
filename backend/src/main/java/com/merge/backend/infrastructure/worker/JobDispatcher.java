package com.merge.backend.infrastructure.worker;

import com.merge.backend.infrastructure.queue.JobHandler;
import com.merge.backend.infrastructure.queue.JobPayload;
import com.merge.backend.infrastructure.queue.JobQueueService;
import com.merge.backend.infrastructure.queue.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Executes a single job and manages its retry lifecycle.
 * Polling is handled externally by {@link QueueWorkerPool} — one thread per concurrency slot.
 *
 * Retry backoff: baseBackoffMs * 2^(attempt-1), capped at 5 minutes.
 * Final failure is delegated to the JobErrorHandler (wired by INF-02).
 */
@Component
public class JobDispatcher {

    private static final Logger log = LoggerFactory.getLogger(JobDispatcher.class);
    private static final long MAX_BACKOFF_MS = 5 * 60 * 1000L;

    @Value("${job.queue.retry-base-backoff-ms:2000}")
    private long baseBackoffMs;

    private final JobQueueService jobQueue;
    private final Map<JobType, JobHandler> handlers;

    /**
     * finalFailureHandler is optional here — it is provided by INF-02.
     * If absent, final failures are logged only.
     */
    private final FinalFailureHandler finalFailureHandler;

    public JobDispatcher(JobQueueService jobQueue,
                         List<JobHandler> handlerBeans,
                         FinalFailureHandler finalFailureHandler) {
        this.jobQueue = jobQueue;
        this.finalFailureHandler = finalFailureHandler;

        Map<JobType, JobHandler> map = new EnumMap<>(JobType.class);
        for (JobHandler h : handlerBeans) {
            map.put(h.jobType(), h);
        }
        this.handlers = map;
    }

    /**
     * Processes one job: invokes its handler, schedules a retry on failure,
     * or delegates to the final-failure handler when attempts are exhausted.
     */
    public void process(JobPayload job) {
        job.setLastAttemptAt(Instant.now());
        log.debug("[JobDispatcher] Processing type={} jobId={} attempt={}",
                job.getJobType(), job.getJobId(), job.getAttemptCount() + 1);

        try {
            JobHandler handler = handlers.get(job.getJobType());
            if (handler == null) {
                throw new IllegalStateException("No handler registered for " + job.getJobType());
            }
            handler.handle(job);
            log.debug("[JobDispatcher] Completed type={} jobId={}", job.getJobType(), job.getJobId());

        } catch (Exception ex) {
            job.setLastError(ex.getMessage());
            int attempt = job.getAttemptCount() + 1;
            job.setAttemptCount(attempt);

            int maxAttempts = job.getJobType().maxAttempts;

            if (maxAttempts > 0 && attempt < maxAttempts) {
                long delay = Math.min(baseBackoffMs * (1L << (attempt - 1)), MAX_BACKOFF_MS);
                log.warn("[JobDispatcher] Scheduling retry type={} jobId={} attempt={}/{} in {}ms",
                        job.getJobType(), job.getJobId(), attempt, maxAttempts, delay);
                jobQueue.scheduleRetry(job, delay);
            } else {
                log.error("[JobDispatcher] Final failure type={} jobId={} attempts={}",
                        job.getJobType(), job.getJobId(), attempt, ex);
                finalFailureHandler.onFinalFailure(job, ex);
            }
        }
    }
}
