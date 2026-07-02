package com.merge.backend.assessment.dto;

import com.merge.backend.assessment.domain.BuildGateResult;

public record BuildGateResultDto(
        String gate,
        String status,
        String feedback
) {
    public static BuildGateResultDto from(BuildGateResult result) {
        return new BuildGateResultDto(
                result.getGate().name(),
                result.getStatus().name(),
                result.getFeedback()
        );
    }
}
