package com.merge.backend.infrastructure.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JobQueueService {

    static final String MAIN_QUEUE_KEY = "merge:queue:main";
    static final String RETRY_ZSET_KEY = "merge:queue:retry";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public JobQueueService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

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
        push(MAIN_QUEUE_KEY, job);
    }

    /** Re-queues a failed job after exponential backoff delay (milliseconds). Caller must have already incremented attemptCount. */
    public void scheduleRetry(JobPayload job, long delayMs) {
        double score = Instant.now().toEpochMilli() + delayMs;
        try {
            redis.opsForZSet().add(RETRY_ZSET_KEY, objectMapper.writeValueAsString(job), score);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialise job for retry", e);
        }
    }

    /** Moves jobs whose retry time has arrived back onto the main queue. */
    public void promoteReadyRetries() {
        double now = Instant.now().toEpochMilli();
        var ready = redis.opsForZSet().rangeByScore(RETRY_ZSET_KEY, 0, now);
        if (ready == null || ready.isEmpty()) return;

        for (String raw : ready) {
            redis.opsForZSet().remove(RETRY_ZSET_KEY, raw);
            redis.opsForList().rightPush(MAIN_QUEUE_KEY, raw);
        }
    }

    /** Non-blocking poll — returns null if the queue is empty. */
    public JobPayload poll() {
        String raw = redis.opsForList().leftPop(MAIN_QUEUE_KEY);
        if (raw == null) return null;
        try {
            return objectMapper.readValue(raw, JobPayload.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialise job from queue", e);
        }
    }

    private void push(String key, JobPayload job) {
        try {
            redis.opsForList().rightPush(key, objectMapper.writeValueAsString(job));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialise job", e);
        }
    }
}
