package com.merge.backend.assessment.judge0;

/** Internal result after Judge0 polling completes. */
public record Judge0Outcome(int statusId, String stderr) {

    public boolean testsPassed() {
        return statusId == 3;
    }
}
