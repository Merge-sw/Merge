package com.merge.backend.progression.service;

import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.progression.domain.ActivityType;
import com.merge.backend.progression.domain.XpEntry;
import com.merge.backend.progression.repository.XpEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class ProgressionServiceImpl implements ProgressionService {

    /**
     * Maximum XP a student can earn from a given activity type within one stage.
     * Unlisted activity types are uncapped.
     */
    private static final Map<ActivityType, Integer> STAGE_CAPS = Map.of(
            ActivityType.LEARNING_RESOURCE, 50
    );

    private final XpEntryRepository xpEntryRepository;
    private final StudentRepository studentRepository;

    public ProgressionServiceImpl(XpEntryRepository xpEntryRepository,
                                  StudentRepository studentRepository) {
        this.xpEntryRepository = xpEntryRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    @Transactional
    public int awardXp(Student student, int xpValue, ActivityType activityType,
                       String stageType, Long sourceId) {
        int cap = STAGE_CAPS.getOrDefault(activityType, Integer.MAX_VALUE);

        int alreadyEarned = xpEntryRepository.sumByStudentIdAndStageTypeAndActivityType(
                student.getId(), stageType, activityType);

        int remaining = cap - alreadyEarned;
        if (remaining <= 0) {
            return 0;
        }

        int actual = Math.min(xpValue, remaining);

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

        return actual;
    }
}
