package com.merge.backend.curriculum.dto;

import com.merge.backend.curriculum.domain.Stage;

public record StageResponse(
        String name,
        int xpThreshold,
        int buildPassScoreThreshold,
        String aiAccessLevel,
        boolean hasSyntaxExercises,
        boolean hasPeerReview,
        String cleanCodeLevel
) {
    public static StageResponse from(Stage stage) {
        return new StageResponse(
                stage.getName(),
                stage.getXpThreshold(),
                stage.getBuildPassScoreThreshold(),
                stage.getAiAccessLevel(),
                stage.isHasSyntaxExercises(),
                stage.isHasPeerReview(),
                stage.getCleanCodeLevel()
        );
    }
}
