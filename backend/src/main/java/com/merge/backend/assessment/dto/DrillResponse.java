package com.merge.backend.assessment.dto;

import com.merge.backend.assessment.domain.Drill;

public record DrillResponse(
        Long id,
        int drillNumber,
        String problemStatement,
        String starterCode,
        boolean locked,
        /**
         * Drill 2 only: true once the student has submitted a code reading response.
         * Frontend must not show the code editor until this is true.
         * Always false for Drill 1 (code reading not required).
         */
        boolean codeReadingCompleted
) {
    public static DrillResponse from(Drill drill, boolean locked, boolean codeReadingCompleted) {
        return new DrillResponse(
                drill.getId(),
                drill.getDrillNumber(),
                drill.getProblemStatement(),
                drill.getStarterCode(),
                locked,
                codeReadingCompleted
        );
    }
}
