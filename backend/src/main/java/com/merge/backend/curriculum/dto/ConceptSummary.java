package com.merge.backend.curriculum.dto;

import com.merge.backend.curriculum.domain.Concept;

public record ConceptSummary(
        Long id,
        String name,
        int sequenceOrder,
        String sfiaSkill,
        boolean isActive,
        boolean unlocked
) {
    public static ConceptSummary from(Concept concept, boolean unlocked) {
        return new ConceptSummary(
                concept.getId(),
                concept.getName(),
                concept.getSequenceOrder(),
                concept.getSfiaSkill(),
                concept.isActive(),
                unlocked
        );
    }
}
