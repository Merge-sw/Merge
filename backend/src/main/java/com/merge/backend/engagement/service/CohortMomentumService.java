package com.merge.backend.engagement.service;

import com.merge.backend.engagement.domain.MomentumState;
import com.merge.backend.engagement.domain.WeeklyMomentum;
import com.merge.backend.engagement.dto.CohortMomentumEntry;
import com.merge.backend.engagement.repository.WeeklyMomentumRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CohortMomentumService {

    private final StudentRepository studentRepository;
    private final WeeklyMomentumRepository weeklyMomentumRepository;

    public CohortMomentumService(StudentRepository studentRepository,
                                 WeeklyMomentumRepository weeklyMomentumRepository) {
        this.studentRepository = studentRepository;
        this.weeklyMomentumRepository = weeklyMomentumRepository;
    }

    @Transactional(readOnly = true)
    public List<CohortMomentumEntry> getCohortMomentum() {
        Map<Long, MomentumState> latestStates = weeklyMomentumRepository.findLatestPerStudent()
                .stream()
                .collect(Collectors.toMap(
                        wm -> wm.getStudent().getId(),
                        WeeklyMomentum::getState));

        return studentRepository.findAll()
                .stream()
                .map(student -> new CohortMomentumEntry(
                        initials(student.getName()),
                        latestStates.getOrDefault(student.getId(), MomentumState.OFFLINE).name(),
                        student.getTotalXp()))
                .collect(Collectors.toList());
    }

    /** Derives initials by taking the first character of each whitespace-separated word. */
    private static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        return Arrays.stream(name.trim().split("\\s+"))
                .filter(w -> !w.isEmpty())
                .map(w -> String.valueOf(w.charAt(0)).toUpperCase())
                .collect(Collectors.joining());
    }
}
