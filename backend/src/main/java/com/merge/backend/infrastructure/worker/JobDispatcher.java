package com.merge.backend.infrastructure.worker;

import com.merge.backend.infrastructure.error.JobErrorHandler;
import com.merge.backend.infrastructure.queue.JobHandler;
import com.merge.backend.infrastructure.queue.JobPayload;
import com.merge.backend.infrastructure.queue.JobQueueService;
import com.merge.backend.infrastructure.queue.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Polls the main queue, dispatches to the correct JobHandler, and manages
 * exponential-backoff retries. Hands finally-failed jobs to JobErrorHandler.
 *
 * Retry backoff: baseBackoffMs * 2^(attempt - 1), capped at 5 minutes.
 */
@Component
@EnableScheduling
public class JobDispatcher {

    private static final Logger log = LoggerFactory.getLogger(JobDispatcher.class);
    private static final long MAX_BACKOFF_MS = 5 * 60 * 1000L;

    @Value("${job.queue.retry-base-backoff-ms:2000}")
    private long baseBackoffMs;

    private final JobQueueService jobQueue;
    private final JobErrorHandler errorHandler;
    private final Map<JobType, JobHandler> handlers;

    public JobDispatcher(JobQueueService jobQueue,
                         JobErrorHandler errorHandler,
                         List<JobHandler> handlerBeans) {
        this.jobQueue = jobQueue;
        this.errorHandler = errorHandler;

        Map<JobType, JobHandler> map = new EnumMap<>(JobType.class);
        for (JobHandler h : handlerBeans) {
            map.put(h.jobType(), h);
        }
        this.handlers = map;
    }

    /** Promotes delayed-retry jobs that are now due back onto the main queue. */
    @Scheduled(fixedDelayString = "${job.queue.poll-interval-ms:500}")
    public void promoteRetries() {
        jobQueue.promoteReadyRetries();
    }

    /** Drains one job per tick from the main queue. */
    @Scheduled(fixedDelayString = "${job.queue.poll-interval-ms:500}")
    public void poll() {
        JobPayload job = jobQueue.poll();
        if (job == null) return;

        job.setLastAttemptAt(Instant.now());
        log.debug("[JobDispatcher] Dequeued type={} jobId={} attempt={}",
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
                log.warn("[JobDispatcher] Retrying type={} jobId={} attempt={}/{} in {}ms",
                        job.getJobType(), job.getJobId(), attempt, maxAttempts, delay);
                jobQueue.scheduleRetry(job, delay);
            } else {
                errorHandler.onFinalFailure(job, ex);
            }
        }
    }
}
