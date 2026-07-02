package com.merge.backend.assessment.dto;

import com.merge.backend.assessment.domain.Drill;

public record DrillResponse(
        Long id,
        int drillNumber,
        String problemStatement,
        String starterCode,
        boolean locked
) {
    public static DrillResponse from(Drill drill, boolean locked) {
        return new DrillResponse(
                drill.getId(),
                drill.getDrillNumber(),
                drill.getProblemStatement(),
                drill.getStarterCode(),
                locked
        );
    }
}
