package com.merge.backend.ai.embedding;

import com.merge.backend.personalisation.domain.PersonalisationProfile;
import com.merge.backend.personalisation.repository.PersonalisationProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class EmbeddingUpdateServiceImpl implements EmbeddingUpdateService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingUpdateServiceImpl.class);

    private final PersonalisationProfileRepository profileRepository;

    public EmbeddingUpdateServiceImpl(PersonalisationProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public void triggerPersonalisationEmbeddingUpdate(Long studentId) {
        log.info("[EmbeddingUpdate] Triggering embedding update for studentId={}", studentId);

        PersonalisationProfile profile = profileRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Personalisation profile not found for student: " + studentId));

        // Generate 768-dimensional vector representation
        String mockVector = generateMockVector();
        profile.setEmbedding(mockVector);

        profileRepository.save(profile);
        log.info("[EmbeddingUpdate] Vector embedding updated for studentId={}", studentId);
    }

    private String generateMockVector() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 768; i++) {
            sb.append(String.format(Locale.US, "%.5f", (float) i / 768.0));
            if (i < 767) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
