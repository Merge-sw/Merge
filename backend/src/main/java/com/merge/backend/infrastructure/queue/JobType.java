package com.merge.backend.infrastructure.queue;

public enum JobType {

    COMPETENCY_SIGNAL(JobCriticality.CRITICAL, 3),
    BUILD_PRD_GENERATION(JobCriticality.CRITICAL, 3),

    PERSONALISATION_UPDATE(JobCriticality.NON_CRITICAL, 2),
    AUDIO_GENERATION(JobCriticality.NON_CRITICAL, 2),
    DISENGAGEMENT_CHECK(JobCriticality.NON_CRITICAL, 2),

    MOMENTUM_CALCULATION(JobCriticality.SCHEDULED, 0),
    SEASON_LOCK(JobCriticality.SCHEDULED, 0);

    public final JobCriticality criticality;
    /** Maximum delivery attempts before the job is considered finally failed. */
    public final int maxAttempts;

    JobType(JobCriticality criticality, int maxAttempts) {
        this.criticality = criticality;
        this.maxAttempts = maxAttempts;
    }
}
