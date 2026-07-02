package com.merge.backend.curriculum.dto;

import com.merge.backend.curriculum.domain.ConceptResource;

public record ConceptResourceResponse(
        Long id,
        String title,
        String type,
        String source,
        String url,
        Integer durationMinutes,
        boolean isRecap,
        int xpValue,
        boolean offlineAvailable,
        String transcriptUrl
) {
    public static ConceptResourceResponse from(ConceptResource r) {
        return new ConceptResourceResponse(
                r.getId(),
                r.getTitle(),
                r.getType().name(),
                r.getSource(),
                r.getUrl(),
                r.getDurationMinutes(),
                r.isRecap(),
                r.getXpValue(),
                r.isOfflineAvailable(),
                r.getTranscriptUrl()
        );
    }
}
