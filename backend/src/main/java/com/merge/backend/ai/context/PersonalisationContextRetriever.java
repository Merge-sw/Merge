package com.merge.backend.ai.context;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Called by AI-01 before every Gemini call.
 *
 * Runs a pgvector cosine-distance query against personalisation_profiles to find
 * the 5 most similar learner embeddings, then assembles them into a prompt-ready
 * context block that makes every Gemini response specific to the student's history.
 *
 * Query:
 *   SELECT id, student_id, coding_style_patterns, hint_usage_pattern, weak_concepts
 *   FROM personalisation_profiles
 *   ORDER BY embedding <=> ?::vector
 *   LIMIT 5
 */
@Service
@Transactional(readOnly = true)
public class PersonalisationContextRetriever {

    private static final Logger log = LoggerFactory.getLogger(PersonalisationContextRetriever.class);

    private static final String SIMILARITY_QUERY = """
            SELECT id,
                   student_id,
                   coding_style_patterns::text,
                   hint_usage_pattern::text,
                   weak_concepts::text
            FROM personalisation_profiles
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT 5
            """;

    private static final TypeReference<List<String>>         LIST_STRING = new TypeReference<>() {};
    private static final TypeReference<Map<String, Integer>> MAP_INT     = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>>  MAP_OBJ     = new TypeReference<>() {};

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    public PersonalisationContextRetriever(EntityManager entityManager, ObjectMapper objectMapper) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    /**
     * Retrieves up to 5 personalisation profiles most similar to {@code queryEmbeddingJson}.
     *
     * @param queryEmbeddingJson the query vector as a JSON-array string, e.g. "[0.1,0.2,...]"
     * @return a {@link PersonalisationContext} containing the entries and a pre-built prompt block
     */
    public PersonalisationContext retrieve(String queryEmbeddingJson) {
        if (queryEmbeddingJson == null || queryEmbeddingJson.isBlank()) {
            return new PersonalisationContext(Collections.emptyList(), "");
        }

        try {
            Query query = entityManager.createNativeQuery(SIMILARITY_QUERY);
            query.setParameter("embedding", queryEmbeddingJson);

            @SuppressWarnings("unchecked")
            List<Object[]> rows = query.getResultList();

            List<PersonalisationContextEntry> entries = rows.stream()
                    .map(this::mapRow)
                    .toList();

            return new PersonalisationContext(entries, buildContextSummary(entries));

        } catch (Exception e) {
            log.error("[PersonalisationContextRetriever] Vector similarity query failed: {}",
                    e.getMessage(), e);
            return new PersonalisationContext(Collections.emptyList(), "");
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private PersonalisationContextEntry mapRow(Object[] row) {
        Long profileId = ((Number) row[0]).longValue();
        Long studentId = ((Number) row[1]).longValue();
        String stylesJson = (String) row[2];
        String hintsJson  = (String) row[3];
        String weakJson   = (String) row[4];

        try {
            List<String>          weakConcepts        = weakJson   != null ? objectMapper.readValue(weakJson,   LIST_STRING) : List.of();
            Map<String, Integer>  hintUsagePattern    = hintsJson  != null ? objectMapper.readValue(hintsJson,  MAP_INT)     : Map.of();
            Map<String, Object>   codingStylePatterns = stylesJson != null ? objectMapper.readValue(stylesJson, MAP_OBJ)     : Map.of();
            return new PersonalisationContextEntry(profileId, studentId, weakConcepts, hintUsagePattern, codingStylePatterns);
        } catch (Exception e) {
            log.warn("[PersonalisationContextRetriever] Failed to parse row profileId={}: {}",
                    profileId, e.getMessage());
            return new PersonalisationContextEntry(profileId, studentId, List.of(), Map.of(), Map.of());
        }
    }

    private String buildContextSummary(List<PersonalisationContextEntry> entries) {
        if (entries.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Learner context — ").append(entries.size()).append(" similar profile(s) found:\n");

        for (int i = 0; i < entries.size(); i++) {
            PersonalisationContextEntry e = entries.get(i);
            sb.append(i + 1).append(". ");
            if (!e.weakConcepts().isEmpty())        sb.append("Weak areas: ").append(e.weakConcepts()).append(". ");
            if (!e.hintUsagePattern().isEmpty())     sb.append("Hint-heavy concepts: ").append(e.hintUsagePattern()).append(". ");
            if (!e.codingStylePatterns().isEmpty())  sb.append("Coding style signals: ").append(e.codingStylePatterns()).append(".");
            sb.append("\n");
        }

        return sb.toString().trim();
    }
}
