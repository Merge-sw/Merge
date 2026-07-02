package com.merge.backend.progression.service;

import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.progression.domain.ActivityType;
import com.merge.backend.progression.domain.XpEntry;
import com.merge.backend.progression.dto.XpAwardResult;
import com.merge.backend.progression.repository.XpEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class ProgressionServiceImpl implements ProgressionService {

    /**
     * Maximum XP a student can earn from a given activity type within one stage.
     *
     * BUILD_PASS cap is generous (10 × DISTINCTION tier) so normal completion paths
     * are never blocked. DRILL_PASS allows full concept coverage per stage.
     * LEARNING_RESOURCE is intentionally low — these are supplementary, not primary XP.
     * Unlisted activity types are uncapped.
     */
    private static final Map<ActivityType, Integer> STAGE_CAPS = Map.of(
            ActivityType.LEARNING_RESOURCE,   50,
            ActivityType.DRILL_PASS,         600,
            ActivityType.BUILD_PASS,        5000,
            ActivityType.PEER_REVIEW,        500,
            ActivityType.STREAK_BONUS,       200
    );

    private final XpEntryRepository xpEntryRepository;
    private final StudentRepository studentRepository;

    public ProgressionServiceImpl(XpEntryRepository xpEntryRepository,
                                  StudentRepository studentRepository) {
        this.xpEntryRepository = xpEntryRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * Awards XP atomically:
     *
     *   BEGIN TRANSACTION (or joins caller's transaction)
     *   SELECT total_xp FROM students WHERE id = ? FOR UPDATE   ← pessimistic write lock
     *   sum xp_entries for (student, stageType, activityType)
     *   actualAward = MIN(amount, remainingCap)
     *   INSERT INTO xp_entries
     *   UPDATE students.total_xp
     *   COMMIT
     */
    @Override
    @Transactional
    public XpAwardResult awardXp(Long studentId, int amount, ActivityType activityType,
                                 String stageType, Long sourceId) {
        // Pessimistic write lock: prevents concurrent XP awards from double-counting.
        // Hibernate executes SELECT … FOR UPDATE; merges fresh total_xp into the session.
        Student student = studentRepository.findByIdForUpdate(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        int cap = STAGE_CAPS.getOrDefault(activityType, Integer.MAX_VALUE);

        int alreadyEarned = xpEntryRepository.sumByStudentIdAndStageTypeAndActivityType(
                studentId, stageType, activityType);

        int remaining = cap - alreadyEarned;
        if (remaining <= 0) {
            return XpAwardResult.atCap();
        }

        int actual = Math.min(amount, remaining);
        boolean capped = actual < amount;

        XpEntry entry = new XpEntry();
        entry.setStudent(student);
        entry.setStageType(stageType);
        entry.setActivityType(activityType);
        entry.setXpAmount(actual);
        entry.setSourceId(sourceId);
        entry.setEarnedAt(Instant.now());
        xpEntryRepository.save(entry);

        student.setTotalXp(student.getTotalXp() + actual);
        studentRepository.save(student);

        return new XpAwardResult(actual, capped);
    }
}
