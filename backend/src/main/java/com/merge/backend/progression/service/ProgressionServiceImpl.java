package com.merge.backend.progression.service;

import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.progression.domain.ActivityType;
import com.merge.backend.progression.domain.XpEntry;
import com.merge.backend.progression.dto.XpAwardResult;
import com.merge.backend.progression.repository.XpCapRepository;
import com.merge.backend.progression.repository.XpEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ProgressionServiceImpl implements ProgressionService {

    private final XpEntryRepository xpEntryRepository;
    private final XpCapRepository xpCapRepository;
    private final StudentRepository studentRepository;

    public ProgressionServiceImpl(XpEntryRepository xpEntryRepository,
                                  XpCapRepository xpCapRepository,
                                  StudentRepository studentRepository) {
        this.xpEntryRepository = xpEntryRepository;
        this.xpCapRepository = xpCapRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    @Transactional
    public XpAwardResult awardXp(Long studentId, int amount, ActivityType activityType,
                                 String stageType, Long sourceId, double decayRate) {
        Student student = studentRepository.findByIdForUpdate(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        int decayed = (int)(amount * decayRate);

        // Read cap from xp_caps table. No row = uncapped (Integer.MAX_VALUE).
        int cap = xpCapRepository
                .findByStageNameAndActivityType(stageType, activityType)
                .map(c -> c.getCapAmount())
                .orElse(Integer.MAX_VALUE);

        int alreadyEarned = xpEntryRepository.sumByStudentIdAndStageTypeAndActivityType(
                studentId, stageType, activityType);

        int remaining = cap - alreadyEarned;
        if (remaining <= 0) {
            return XpAwardResult.atCap();
        }

        int actual = Math.min(decayed, remaining);
        boolean capped = actual < decayed;

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
