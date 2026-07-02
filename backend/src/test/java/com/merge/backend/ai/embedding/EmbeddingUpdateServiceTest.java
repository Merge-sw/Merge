package com.merge.backend.ai.embedding;

import com.merge.backend.personalisation.domain.PersonalisationProfile;
import com.merge.backend.personalisation.repository.PersonalisationProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EmbeddingUpdateServiceTest {

    private EmbeddingUpdateService embeddingUpdateService;

    @Mock
    private PersonalisationProfileRepository profileRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        embeddingUpdateService = new EmbeddingUpdateServiceImpl(profileRepository);
    }

    @Test
    public void testTriggerPersonalisationEmbeddingUpdate() {
        PersonalisationProfile profile = new PersonalisationProfile();
        profile.setId(1L);
        profile.setEmbedding(null);

        when(profileRepository.findByStudentId(42L)).thenReturn(Optional.of(profile));

        embeddingUpdateService.triggerPersonalisationEmbeddingUpdate(42L);

        assertNotNull(profile.getEmbedding());
        assertTrue(profile.getEmbedding().startsWith("["));
        assertTrue(profile.getEmbedding().endsWith("]"));

        // Count commas to verify 768 elements
        String[] elements = profile.getEmbedding().substring(1, profile.getEmbedding().length() - 1).split(",");
        assertEquals(768, elements.length);

        verify(profileRepository, times(1)).save(profile);
    }
}
