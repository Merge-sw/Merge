package com.merge.backend.ai.embedding;

/**
 * AI-06: Generates and stores an updated vector embedding for a student's
 * personalisation profile. Called after any profile write.
 */
public interface EmbeddingUpdateService {
    void triggerPersonalisationEmbeddingUpdate(Long studentId);
}
