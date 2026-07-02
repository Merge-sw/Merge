package com.merge.backend.infrastructure.queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobPayload {

    private String jobId;
    private JobType jobType;

    /** Module-specific data serialised as a JSON string. */
    private String payloadJson;

    private int attemptCount;
    private Instant enqueuedAt;
    private Instant lastAttemptAt;
    private String lastError;
}
