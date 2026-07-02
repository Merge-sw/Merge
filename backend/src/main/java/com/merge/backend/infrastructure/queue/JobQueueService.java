package com.merge.backend.infrastructure.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class JobQueueService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public JobQueueService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** Enqueues a new job to its dedicated queue as defined by {@link JobType#queueKey}. */
    public void enqueue(JobType type, String payloadJson) {
        JobPayload job = new JobPayload(
                UUID.randomUUID().toString(),
                type,
                payloadJson,
                0,
                Instant.now(),
                null,
                null
        );
        push(type.queueKey, job);
    }

    /**
     * Schedules a failed job for retry via a Redis sorted set.
     * Score = epoch millis at which the job becomes eligible.
     */
    public void scheduleRetry(JobPayload job, long delayMs) {
        double score = Instant.now().toEpochMilli() + delayMs;
        try {
            redis.opsForZSet().add(QueueNames.RETRY, objectMapper.writeValueAsString(job), score);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialise job for retry queue", e);
        }
    }

    /**
     * Moves all retry-eligible jobs (score ≤ now) back to their original per-type queue.
     * Called on a fixed schedule by the worker pool.
     */
    public void promoteReadyRetries() {
        double now = Instant.now().toEpochMilli();
        Set<String> ready = redis.opsForZSet().rangeByScore(QueueNames.RETRY, 0, now);
        if (ready == null || ready.isEmpty()) return;

        for (String raw : ready) {
            redis.opsForZSet().remove(QueueNames.RETRY, raw);
            try {
                JobPayload job = objectMapper.readValue(raw, JobPayload.class);
                redis.opsForList().rightPush(job.getJobType().queueKey, raw);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Corrupt entry in retry queue — cannot promote", e);
            }
        }
    }

    /**
     * Non-blocking poll from the given queue key. Returns null when the queue is empty.
     * Each worker thread calls this with its assigned queue key.
     */
    public JobPayload poll(String queueKey) {
        String raw = redis.opsForList().leftPop(queueKey);
        if (raw == null) return null;
        try {
            return objectMapper.readValue(raw, JobPayload.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialise job from queue " + queueKey, e);
        }
    }

    private void push(String key, JobPayload job) {
        try {
            redis.opsForList().rightPush(key, objectMapper.writeValueAsString(job));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialise job for queue " + key, e);
        }
    }
}
