package com.merge.backend.identity.dto;

public record PromoteResponse(
        String fromStage,
        String toStage,
        int xpAtPromotion,
        int buildScoreAtPromotion
) {}
