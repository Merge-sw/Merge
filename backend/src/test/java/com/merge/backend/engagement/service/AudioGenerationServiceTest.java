package com.merge.backend.engagement.service;

import com.merge.backend.ai.gateway.GeminiGateway;
import com.merge.backend.curriculum.domain.Concept;
import com.merge.backend.curriculum.repository.ConceptRepository;
import com.merge.backend.engagement.domain.AudioRecord;
import com.merge.backend.engagement.domain.AudioType;
import com.merge.backend.engagement.repository.AudioRecordRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.personalisation.domain.LearningApproach;
import com.merge.backend.personalisation.domain.PersonalisationProfile;
import com.merge.backend.personalisation.repository.PersonalisationProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AudioGenerationServiceTest {

    private AudioGenerationService audioGenerationService;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ConceptRepository conceptRepository;

    @Mock
    private PersonalisationProfileRepository profileRepository;

    @Mock
    private AudioRecordRepository audioRecordRepository;

    @Mock
    private GeminiGateway geminiGateway;

    private Student testStudent;
    private Concept testConcept;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        audioGenerationService = new AudioGenerationServiceImpl(
                studentRepository,
                conceptRepository,
                profileRepository,
                audioRecordRepository,
                geminiGateway
        );

        testStudent = new Student(10L, "Alice Smith", "alice@test.com", "123", "alice@u.edu", "hash", "CADET", 100, null, null, null);
        testConcept = new Concept();
        testConcept.setId(20L);
        testConcept.setName("Variables");

        when(studentRepository.findById(10L)).thenReturn(Optional.of(testStudent));
        when(conceptRepository.findById(20L)).thenReturn(Optional.of(testConcept));
    }

    @Test
    public void testGenerateAudio() {
        PersonalisationProfile profile = new PersonalisationProfile();
        profile.setLearningApproach(LearningApproach.DEFINITIONS_FIRST);
        profile.setWeakConcepts(List.of("Conditionals"));
        when(profileRepository.findByStudentId(10L)).thenReturn(Optional.of(profile));

        when(geminiGateway.generateAudioScript(eq("Variables"), eq("REINFORCEMENT"), eq("DEFINITIONS_FIRST"), anyList()))
                .thenReturn("Generated Script Text");

        when(audioRecordRepository.save(any(AudioRecord.class))).thenAnswer(invocation -> {
            AudioRecord r = invocation.getArgument(0);
            r.setId(5L);
            return r;
        });

        AudioRecord record = audioGenerationService.generateAudio(10L, 20L, AudioType.REINFORCEMENT);

        assertNotNull(record);
        assertEquals(5L, record.getId());
        assertEquals("Generated Script Text", record.getScriptText());
        assertEquals(AudioType.REINFORCEMENT, record.getAudioType());
        assertEquals(testStudent, record.getStudent());
        assertEquals(testConcept, record.getConcept());
        assertFalse(record.isListened());

        verify(audioRecordRepository, times(1)).save(any(AudioRecord.class));
    }
}
