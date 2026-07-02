package com.merge.backend.engagement.service;

import com.merge.backend.assessment.repository.DrillCompletionRepository;
import com.merge.backend.assessment.repository.DrillSubmissionRepository;
import com.merge.backend.engagement.domain.MomentumState;
import com.merge.backend.engagement.domain.WeeklyMomentum;
import com.merge.backend.engagement.repository.SessionRepository;
import com.merge.backend.engagement.repository.WeeklyMomentumRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class MomentumCalculationService {

    private static final Logger log = LoggerFactory.getLogger(MomentumCalculationService.class);
    private static final double DEPLOYING_PASS_RATE_THRESHOLD = 0.9;

    private final StudentRepository studentRepository;
    private final SessionRepository sessionRepository;
    private final DrillCompletionRepository drillCompletionRepository;
    private final DrillSubmissionRepository drillSubmissionRepository;
    private final WeeklyMomentumRepository weeklyMomentumRepository;

    public MomentumCalculationService(StudentRepository studentRepository,
                                      SessionRepository sessionRepository,
                                      DrillCompletionRepository drillCompletionRepository,
                                      DrillSubmissionRepository drillSubmissionRepository,
                                      WeeklyMomentumRepository weeklyMomentumRepository) {
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
        this.drillCompletionRepository = drillCompletionRepository;
        this.drillSubmissionRepository = drillSubmissionRepository;
        this.weeklyMomentumRepository = weeklyMomentumRepository;
    }

    /**
     * Iterates all students and upserts their WeeklyMomentum for the given Monday week.
     * Each student is committed individually so one failure does not abort the whole batch.
     */
    public void calculateAll(LocalDate weekStart) {
        log.info("[MomentumCalculation] Starting batch for week_start={}", weekStart);
        int processed = 0;
        int failed = 0;
        for (Student student : studentRepository.findAll()) {
            try {
                calculateForStudent(student, weekStart);
                processed++;
            } catch (Exception e) {
                log.error("[MomentumCalculation] Failed for student={} week={}: {}",
                        student.getId(), weekStart, e.getMessage(), e);
                failed++;
            }
        }
        log.info("[MomentumCalculation] Completed week={} processed={} failed={}",
                weekStart, processed, failed);
    }

    @Transactional
    public void calculateForStudent(Student student, LocalDate weekStart) {
        Instant weekFrom = weekStart.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant weekTo   = weekStart.plusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant prevFrom = weekStart.minusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();

        int sessionCount     = sessionRepository.countSessionsBetween(student.getId(), weekFrom, weekTo);
        int prevSessionCount = sessionRepository.countSessionsBetween(student.getId(), prevFrom, weekFrom);

        Double drillPassRate = null;
        if (sessionCount >= 3) {
            int attempted = drillSubmissionRepository.countAttemptsBetween(student.getId(), weekFrom, weekTo);
            int passed    = drillCompletionRepository.countPassedBetween(student.getId(), weekFrom, weekTo);
            drillPassRate = attempted > 0 ? (double) passed / attempted : 0.0;
        }

        MomentumState state = deriveState(sessionCount, prevSessionCount, drillPassRate);

        WeeklyMomentum momentum = weeklyMomentumRepository
                .findByStudentIdAndWeekStart(student.getId(), weekStart)
                .orElseGet(WeeklyMomentum::new);

        momentum.setStudent(student);
        momentum.setWeekStart(weekStart);
        momentum.setState(state);
        momentum.setSessionCount(sessionCount);
        momentum.setDrillPassRate(drillPassRate);
        momentum.setCalculatedAt(Instant.now());
        weeklyMomentumRepository.save(momentum);
    }

    private MomentumState deriveState(int sessionCount, int prevSessionCount, Double drillPassRate) {
        if (sessionCount >= 3) {
            return drillPassRate != null && drillPassRate >= DEPLOYING_PASS_RATE_THRESHOLD
                    ? MomentumState.DEPLOYING
                    : MomentumState.BUILDING;
        }
        if (sessionCount >= 1) {
            return MomentumState.COMPILING;
        }
        return prevSessionCount >= 1 ? MomentumState.BLOCKED : MomentumState.OFFLINE;
    }
}
