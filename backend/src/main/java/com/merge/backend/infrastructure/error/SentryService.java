package com.merge.backend.infrastructure.error;

import com.merge.backend.infrastructure.queue.JobPayload;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import org.springframework.stereotype.Service;

@Service
public class SentryService {

    public void captureJobFailure(JobPayload job, Throwable cause, SentryLevel level) {
        SentryEvent event = new SentryEvent(cause);
        event.setLevel(level);

        Message msg = new Message();
        msg.setMessage("Job final failure: " + job.getJobType());
        event.setMessage(msg);

        event.setTag("job.type", job.getJobType().name());
        event.setTag("job.id", job.getJobId());
        event.setTag("job.criticality", job.getJobType().criticality.name());
        event.setTag("job.attempts", String.valueOf(job.getAttemptCount()));

        // Stable fingerprint so Sentry groups all failures of the same job type together
        event.setFingerprints(java.util.List.of("job-failure", job.getJobType().name()));

        Sentry.captureEvent(event);
    }

    public void captureScheduledJobError(JobPayload job, Throwable cause) {
        SentryEvent event = new SentryEvent(cause);
        event.setLevel(SentryLevel.WARNING);

        Message msg = new Message();
        msg.setMessage("Scheduled job error: " + job.getJobType());
        event.setMessage(msg);

        event.setTag("job.type", job.getJobType().name());
        event.setTag("job.id", job.getJobId());
        event.setTag("job.criticality", "SCHEDULED");

        Sentry.captureEvent(event);
    }
}
