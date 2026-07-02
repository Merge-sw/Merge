package com.merge.backend.curriculum.dto;

import com.merge.backend.curriculum.domain.Concept;

public record ConceptResponse(
        Long id,
        String stageName,
        String name,
        int sequenceOrder,
        String sfiaSkill,
        String failureScenario,
        boolean isActive
) {
    public static ConceptResponse from(Concept concept) {
        return new ConceptResponse(
                concept.getId(),
                concept.getStageName(),
                concept.getName(),
                concept.getSequenceOrder(),
                concept.getSfiaSkill(),
                concept.getFailureScenario(),
                concept.isActive()
        );
    }
}
