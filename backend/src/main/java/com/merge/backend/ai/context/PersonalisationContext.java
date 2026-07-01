package com.merge.backend.ai.context;

import java.util.List;

/**
 * Built by PersonalisationContextRetriever and injected into every AI-01 Gemini call.
 * {@code contextSummary} is a pre-formatted text block ready to prepend to a Gemini prompt.
 */
public record PersonalisationContext(
        List<PersonalisationContextEntry> similarProfiles,
        String contextSummary
) {
    public boolean isEmpty() {
        return similarProfiles == null || similarProfiles.isEmpty();
    }
}
