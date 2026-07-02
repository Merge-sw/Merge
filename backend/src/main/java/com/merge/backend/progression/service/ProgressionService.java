package com.merge.backend.progression.service;

import com.merge.backend.identity.domain.Student;
import com.merge.backend.progression.domain.ActivityType;

/**
 * XP award gateway consumed by feature modules.
 * Enforces per-stage activity caps and appends to the xp_entries ledger.
 */
public interface ProgressionService {

    /**
     * Awards XP to a student for a completed activity, subject to per-stage caps.
     *
     * @param student      the student receiving XP
     * @param xpValue      requested XP amount
     * @param activityType type of activity driving the award
     * @param stageType    stage the student currently holds (used for cap bucket)
     * @param sourceId     optional reference id of the source entity (resource, drill, etc.)
     * @return actual XP credited (0 if the stage cap for this activity type is already reached)
     */
    int awardXp(Student student, int xpValue, ActivityType activityType,
                String stageType, Long sourceId);
}
