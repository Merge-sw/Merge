package com.merge.backend.progression.service;

import com.merge.backend.progression.domain.ActivityType;
import com.merge.backend.progression.dto.XpAwardResult;

/**
 * XP award gateway consumed by feature modules.
 * Enforces per-stage activity caps and appends to the xp_entries ledger.
 *
 * XP is never awarded from the frontend — all award calls originate from
 * server-side service logic triggered by verified activity completion events.
 */
public interface ProgressionService {

    /**
     * Awards XP to a student for a completed activity, subject to per-stage caps.
     *
     * Execution sequence (all within one transaction):
     *   1. SELECT total_xp FROM students WHERE id = ? FOR UPDATE
     *   2. decayedAmount = (int)(amount * decayRate)
     *   3. Sum existing xp_entries for (student, stageType, activityType) to find remaining cap
     *   4. actualAward = MIN(decayedAmount, remainingCap)
     *   5. INSERT INTO xp_entries
     *   6. UPDATE students SET total_xp = total_xp + actualAward
     *
     * @param studentId    ID of the student receiving XP
     * @param amount       base XP amount before decay
     * @param activityType type of activity driving the award
     * @param stageType    stage the student currently holds (used for cap bucket)
     * @param sourceId     optional reference id of the source entity (resource, drill, build, etc.)
     * @param decayRate    multiplier from {@link #decayRate(int)}: 1.0 / 0.75 / 0.50 / 0.25
     * @return XpAwardResult with the actual XP credited and whether the cap was hit
     */
    XpAwardResult awardXp(Long studentId, int amount, ActivityType activityType,
                          String stageType, Long sourceId, double decayRate);

    /**
     * Returns the XP decay multiplier for a given attempt number.
     * 1st attempt: 100%, 2nd: 75%, 3rd: 50%, 4th and beyond: 25%.
     */
    static double decayRate(int attemptNumber) {
        return switch (attemptNumber) {
            case 1 -> 1.0;
            case 2 -> 0.75;
            case 3 -> 0.50;
            default -> 0.25;
        };
    }
}
