package com.merge.backend.infrastructure.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Persists finally-failed jobs to the dead letter queue so no data is lost.
 * Critical jobs remain here until manually replayed or resolved.
 */
@Service
public class DeadLetterQueueService {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueService.class);
    static final String DLQ_KEY = "merge:queue:dlq";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public DeadLetterQueueService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void park(JobPayload job) {
        try {
            String raw = objectMapper.writeValueAsString(job);
            redis.opsForList().rightPush(DLQ_KEY, raw);
            log.error("[DLQ] Parked job {} type={} attempts={}",
                    job.getJobId(), job.getJobType(), job.getAttemptCount());
        } catch (JsonProcessingException e) {
            log.error("[DLQ] Failed to serialise job {} for DLQ — payload may be lost: {}",
                    job.getJobId(), e.getMessage());
        }
    }

    public List<JobPayload> listAll() {
        List<String> raw = redis.opsForList().range(DLQ_KEY, 0, -1);
        if (raw == null) return Collections.emptyList();
        return raw.stream().map(s -> {
            try {
                return objectMapper.readValue(s, JobPayload.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Corrupt DLQ entry", e);
            }
        }).toList();
    }
}
