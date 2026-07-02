package com.merge.backend.infrastructure.queue;

public enum JobType {

    // Critical — retry 3×, DLQ + alert on final failure
    GITHUB_COMMIT        (JobCriticality.CRITICAL,     3, QueueNames.GITHUB_COMMIT),
    COMPETENCY_SIGNAL    (JobCriticality.CRITICAL,     3, QueueNames.COMPETENCY_SIGNAL),
    BUILD_PRD_GENERATION (JobCriticality.CRITICAL,     3, QueueNames.BUILD_PRD_GENERATION),

    // Non-critical — retry 2×, DLQ + warn on final failure
    CLEAN_CODE_FEEDBACK   (JobCriticality.NON_CRITICAL, 2, QueueNames.CLEAN_CODE_FEEDBACK),
    PERSONALISATION_UPDATE(JobCriticality.NON_CRITICAL, 2, QueueNames.PERSONALISATION_UPDATE),
    AUDIO_GENERATION      (JobCriticality.NON_CRITICAL, 2, QueueNames.AUDIO_GENERATION),
    DISENGAGEMENT_CHECK   (JobCriticality.NON_CRITICAL, 2, QueueNames.DISENGAGEMENT_CHECK),
    SPOT_CHECK_PEER_REVIEW(JobCriticality.NON_CRITICAL, 2, QueueNames.SPOT_CHECK_PEER_REVIEW),

    // Scheduled — no retry, log only
    MOMENTUM_CALCULATION  (JobCriticality.SCHEDULED,    0, QueueNames.MOMENTUM_CALCULATION),
    SEASON_LOCK           (JobCriticality.SCHEDULED,    0, QueueNames.SEASON_LOCK);

    public final JobCriticality criticality;
    /** Maximum delivery attempts before the job is considered finally failed. */
    public final int maxAttempts;
    /** Redis list key for this job type's dedicated queue. */
    public final String queueKey;

    JobType(JobCriticality criticality, int maxAttempts, String queueKey) {
        this.criticality = criticality;
        this.maxAttempts = maxAttempts;
        this.queueKey = queueKey;
    }
}
