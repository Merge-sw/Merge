package com.merge.backend.infrastructure.worker;

import com.merge.backend.infrastructure.config.WorkerProperties;
import com.merge.backend.infrastructure.queue.JobPayload;
import com.merge.backend.infrastructure.queue.JobQueueService;
import com.merge.backend.infrastructure.queue.QueueNames;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Starts one dedicated thread pool per queue, sized by {@link WorkerProperties}.
 * Each thread continuously polls its queue and delegates to {@link JobDispatcher#process}.
 *
 * Queue → concurrency mapping (from application.properties):
 *   github_commit=3, clean_code_feedback=2, personalisation_update=3,
 *   competency_signal=2, build_prd_generation=1, audio_generation=2,
 *   momentum_calculation=1, disengagement_check=3, season_lock=1,
 *   spot_check_peer_review=2
 */
@Component
public class QueueWorkerPool implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QueueWorkerPool.class);

    @Value("${job.queue.poll-interval-ms:500}")
    private long pollIntervalMs;

    private volatile boolean running = true;
    private final List<ExecutorService> pools = new ArrayList<>();

    private final WorkerProperties workerProperties;
    private final JobQueueService jobQueue;
    private final JobDispatcher dispatcher;

    public QueueWorkerPool(WorkerProperties workerProperties,
                           JobQueueService jobQueue,
                           JobDispatcher dispatcher) {
        this.workerProperties = workerProperties;
        this.jobQueue = jobQueue;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run(ApplicationArguments args) {
        Map<String, Integer> concurrencyMap = workerProperties.getConcurrency();

        for (Map.Entry<String, Integer> entry : concurrencyMap.entrySet()) {
            String propertyKey = entry.getKey();
            int concurrency = entry.getValue();

            String redisKey = QueueNames.BY_PROPERTY_KEY.get(propertyKey);
            if (redisKey == null) {
                log.warn("[QueueWorkerPool] Unknown queue property key '{}' — skipping", propertyKey);
                continue;
            }

            ExecutorService pool = Executors.newFixedThreadPool(
                    concurrency,
                    r -> {
                        Thread t = new Thread(r, "worker-" + propertyKey + "-" + Thread.currentThread().getId());
                        t.setDaemon(true);
                        return t;
                    }
            );
            pools.add(pool);

            for (int i = 0; i < concurrency; i++) {
                pool.submit(() -> workerLoop(redisKey, propertyKey));
            }

            log.info("[QueueWorkerPool] Started {} worker(s) for queue '{}'", concurrency, redisKey);
        }
    }

    /** Promotes delayed-retry jobs that are now due back onto their per-type queues. */
    @Scheduled(fixedDelayString = "${job.queue.poll-interval-ms:500}")
    public void promoteRetries() {
        jobQueue.promoteReadyRetries();
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        pools.forEach(ExecutorService::shutdown);
        pools.forEach(pool -> {
            try {
                pool.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        log.info("[QueueWorkerPool] All worker pools shut down");
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void workerLoop(String redisKey, String queueLabel) {
        while (running) {
            try {
                JobPayload job = jobQueue.poll(redisKey);
                if (job != null) {
                    dispatcher.process(job);
                } else {
                    Thread.sleep(pollIntervalMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("[QueueWorkerPool] Unhandled error in worker loop for '{}': {}",
                        queueLabel, e.getMessage(), e);
            }
        }
    }
}
