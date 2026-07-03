package com.merge.backend.assessment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merge.backend.ai.gateway.GeminiGateway;
import com.merge.backend.assessment.domain.*;
import com.merge.backend.assessment.dto.ComprehensionScoreRequest;
import com.merge.backend.assessment.dto.ComprehensionSubmitRequest;
import com.merge.backend.assessment.dto.ComprehensionSubmitResponse;
import com.merge.backend.assessment.repository.ComprehensionCheckRepository;
import com.merge.backend.assessment.repository.DrillCompletionRepository;
import com.merge.backend.curriculum.domain.Concept;
import com.merge.backend.curriculum.service.ConceptUnlockService;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.infrastructure.queue.JobQueueService;
import com.merge.backend.infrastructure.queue.JobType;
import com.merge.backend.progression.dto.XpAwardResult;
import com.merge.backend.progression.service.ProgressionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ComprehensionSubmitServiceTest {

    private ComprehensionSubmitService comprehensionSubmitService;

    @Mock
    private ComprehensionCheckRepository comprehensionCheckRepository;

    @Mock
    private DrillCompletionRepository drillCompletionRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private GeminiGateway geminiGateway;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ConceptUnlockService conceptUnlockService;

    @Mock
    private BuildUnlockService buildUnlockService;

    @Mock
    private JobQueueService jobQueueService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Student testStudent;
    private Concept testConcept;
    private Drill testDrill;
    private DrillSubmission testSubmission;
    private ComprehensionCheck testCheck;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        comprehensionSubmitService = new ComprehensionSubmitService(
                comprehensionCheckRepository,
                drillCompletionRepository,
                studentRepository,
                geminiGateway,
                progressionService,
                conceptUnlockService,
                buildUnlockService,
                jobQueueService,
                objectMapper
        );

        testStudent = new Student(1L, "David", "david@test.com", "123", "david@u.edu", "hash", "CADET", 100, null, null, null);
        testConcept = new Concept();
        testConcept.setId(3L);
        testConcept.setName("Variables");

        testDrill = new Drill();
        testDrill.setId(4L);
        testDrill.setConcept(testConcept);

        testSubmission = new DrillSubmission();
        testSubmission.setId(10L);
        testSubmission.setStudent(testStudent);
        testSubmission.setDrill(testDrill);
        testSubmission.setCode("let x = 10;");

        testCheck = new ComprehensionCheck();
        testCheck.setId(20L);
        testCheck.setStudent(testStudent);
        testCheck.setDrill(testDrill);
        testCheck.setDrillSubmission(testSubmission);
        testCheck.setStatus(ComprehensionCheckStatus.PENDING);
        testCheck.setQuestions(List.of("Q1: What does x do?"));
        testCheck.setServerDeadline(Instant.now().plusSeconds(60));

        when(studentRepository.findByEmail("david@test.com")).thenReturn(Optional.of(testStudent));
        when(comprehensionCheckRepository.findById(20L)).thenReturn(Optional.of(testCheck));
    }

    @Test
    public void testSubmit_CheckNotFound() {
        when(comprehensionCheckRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                comprehensionSubmitService.submit(99L, new ComprehensionSubmitRequest(List.of("Ans")), "david@test.com")
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void testSubmit_StudentNotFound() {
        when(studentRepository.findByEmail("invalid@test.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                comprehensionSubmitService.submit(20L, new ComprehensionSubmitRequest(List.of("Ans")), "invalid@test.com")
        );
    }

    @Test
    public void testSubmit_ForbiddenOwnership() {
        Student otherStudent = new Student(2L, "Other", "other@test.com", "123", "other@u.edu", "hash", "CADET", 100, null, null, null);
        testCheck.setStudent(otherStudent);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                comprehensionSubmitService.submit(20L, new ComprehensionSubmitRequest(List.of("Ans")), "david@test.com")
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    public void testSubmit_AlreadyCompleted() {
        testCheck.setStatus(ComprehensionCheckStatus.PASSED);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                comprehensionSubmitService.submit(20L, new ComprehensionSubmitRequest(List.of("Ans")), "david@test.com")
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    public void testSubmit_TimerExpired() {
        testCheck.setServerDeadline(Instant.now().minusSeconds(10));

        assertThrows(ComprehensionTimerExpiredException.class, () ->
                comprehensionSubmitService.submit(20L, new ComprehensionSubmitRequest(List.of("Ans")), "david@test.com")
        );
        assertEquals(ComprehensionCheckStatus.EXPIRED, testCheck.getStatus());
        verify(comprehensionCheckRepository, times(1)).save(testCheck);
    }

    @Test
    public void testSubmit_ScoringFailed() {
        ComprehensionSubmitRequest req = new ComprehensionSubmitRequest(List.of("My Answer"));

        when(geminiGateway.scoreComprehensionAnswers(any(ComprehensionScoreRequest.class))).thenReturn(false);
        when(comprehensionCheckRepository.save(any(ComprehensionCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComprehensionSubmitResponse res = comprehensionSubmitService.submit(20L, req, "david@test.com");

        assertFalse(res.passed());
        assertEquals(ComprehensionCheckStatus.FAILED, testCheck.getStatus());
        verify(comprehensionCheckRepository, times(2)).save(testCheck);
    }

    @Test
    public void testSubmit_ScoringPassed() {
        ComprehensionSubmitRequest req = new ComprehensionSubmitRequest(List.of("My Answer"));

        when(geminiGateway.scoreComprehensionAnswers(any(ComprehensionScoreRequest.class))).thenReturn(true);
        when(comprehensionCheckRepository.save(any(ComprehensionCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(progressionService.awardXp(anyLong(), anyInt(), any(), anyString(), anyLong(), anyDouble()))
                .thenReturn(new XpAwardResult(50, false));

        ComprehensionSubmitResponse res = comprehensionSubmitService.submit(20L, req, "david@test.com");

        assertTrue(res.passed());
        assertEquals(50, res.xpAwarded());
        assertEquals(ComprehensionCheckStatus.PASSED, testCheck.getStatus());

        // Verify async jobs, unlocks, and completions
        verify(drillCompletionRepository, times(1)).save(any(DrillCompletion.class));
        verify(conceptUnlockService, times(1)).triggerUnlockIfEligible(eq(testStudent), eq(testConcept));
        verify(buildUnlockService, times(1)).checkAndUnlock(eq(testStudent), eq("CADET"));
        verify(jobQueueService, times(1)).enqueue(eq(JobType.CLEAN_CODE_FEEDBACK), anyString());
    }
}
