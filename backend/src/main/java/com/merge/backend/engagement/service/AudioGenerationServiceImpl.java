package com.merge.backend.engagement.service;

import com.merge.backend.ai.gateway.GeminiGateway;
import com.merge.backend.curriculum.domain.Concept;
import com.merge.backend.curriculum.repository.ConceptRepository;
import com.merge.backend.engagement.domain.AudioRecord;
import com.merge.backend.engagement.domain.AudioType;
import com.merge.backend.engagement.repository.AudioRecordRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.personalisation.domain.PersonalisationProfile;
import com.merge.backend.personalisation.repository.PersonalisationProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;

@Service
@Transactional
public class AudioGenerationServiceImpl implements AudioGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AudioGenerationServiceImpl.class);

    private final StudentRepository studentRepository;
    private final ConceptRepository conceptRepository;
    private final PersonalisationProfileRepository profileRepository;
    private final AudioRecordRepository audioRecordRepository;
    private final GeminiGateway geminiGateway;

    public AudioGenerationServiceImpl(StudentRepository studentRepository,
                                       ConceptRepository conceptRepository,
                                       PersonalisationProfileRepository profileRepository,
                                       AudioRecordRepository audioRecordRepository,
                                       GeminiGateway geminiGateway) {
        this.studentRepository = studentRepository;
        this.conceptRepository = conceptRepository;
        this.profileRepository = profileRepository;
        this.audioRecordRepository = audioRecordRepository;
        this.geminiGateway = geminiGateway;
    }

    @Override
    public AudioRecord generateAudio(Long studentId, Long conceptId, AudioType audioType) {
        log.info("[AudioGeneration] Starting script generation for studentId={} conceptId={} type={}",
                studentId, conceptId, audioType);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new IllegalArgumentException("Concept not found: " + conceptId));

        PersonalisationProfile profile = profileRepository.findByStudentId(studentId).orElse(null);

        String learningApproach = profile != null && profile.getLearningApproach() != null
                ? profile.getLearningApproach().name() : "EXAMPLES_FIRST";

        var weakConcepts = profile != null && profile.getWeakConcepts() != null
                ? profile.getWeakConcepts() : Collections.<String>emptyList();

        // Call Gemini AudioWriter prompt
        String script = geminiGateway.generateAudioScript(
                concept.getName(),
                audioType.name(),
                learningApproach,
                weakConcepts
        );

        AudioRecord record = new AudioRecord();
        record.setStudent(student);
        record.setConcept(concept);
        record.setAudioType(audioType);
        record.setScriptText(script);
        record.setGeneratedAt(Instant.now());
        record.setListened(false);

        AudioRecord saved = audioRecordRepository.save(record);
        log.info("[AudioGeneration] Script generated and saved with recordId={}", saved.getId());
        return saved;
    }
}
