package com.merge.backend.curriculum.dto;

import com.merge.backend.curriculum.domain.SyntaxExercise;

public record SyntaxExerciseResponse(
        Long id,
        String brokenCode,
        String instructions,
        int durationMinutes
) {
    public static SyntaxExerciseResponse from(SyntaxExercise exercise) {
        return new SyntaxExerciseResponse(
                exercise.getId(),
                exercise.getBrokenCode(),
                exercise.getInstructions(),
                exercise.getDurationMinutes()
        );
    }
}
