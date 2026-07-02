package com.merge.backend.assessment.service;

import com.merge.backend.assessment.domain.Build;
import com.merge.backend.assessment.domain.BuildSubmission;
import com.merge.backend.assessment.dto.GithubCommitJobPayload;
import com.merge.backend.assessment.repository.BuildRepository;
import com.merge.backend.assessment.repository.BuildSubmissionRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.identity.service.TokenEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

public class GithubCommitServiceTest {

    private GithubCommitService githubCommitService;

    @Mock
    private BuildSubmissionRepository buildSubmissionRepository;

    @Mock
    private BuildRepository buildRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TokenEncryptionService tokenEncryptionService;

    @Mock
    private RestTemplate restTemplate;

    private Student testStudent;
    private Build testBuild;
    private BuildSubmission testSubmission;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        githubCommitService = new GithubCommitServiceImpl(
                buildSubmissionRepository,
                buildRepository,
                studentRepository,
                tokenEncryptionService,
                restTemplate
        );

        testStudent = new Student(1L, "David", "david@test.com", "123", "david@u.edu", "hash", "CADET", 100, "token-encrypted", "my-repo", null);
        testBuild = new Build();
        testBuild.setId(5L);
        testBuild.setStageName("CADET");

        testSubmission = new BuildSubmission();
        testSubmission.setId(10L);
        testSubmission.setStudent(testStudent);
        testSubmission.setBuild(testBuild);
        testSubmission.setCode("console.log('hello world');");

        when(buildSubmissionRepository.findById(10L)).thenReturn(Optional.of(testSubmission));
        when(buildRepository.findById(5L)).thenReturn(Optional.of(testBuild));
    }

    @Test
    public void testProcessCommitJob_NoToken() throws Exception {
        testStudent.setGithubOauthTokenEncrypted(null);

        githubCommitService.processCommitJob(new GithubCommitJobPayload(10L, 1L, 5L));

        assertNull(testSubmission.getCommitHash());
        verify(buildSubmissionRepository, never()).save(any());
    }

    @Test
    public void testProcessCommitJob_SuccessNewFile() throws Exception {
        when(tokenEncryptionService.decrypt("token-encrypted")).thenReturn("decrypted-token");

        // Mock GET /user details
        Map<String, Object> userBody = new HashMap<>();
        userBody.put("login", "david-gh");
        ResponseEntity<Map> userResponse = new ResponseEntity<>(userBody, HttpStatus.OK);
        when(restTemplate.exchange(eq("https://api.github.com/user"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(userResponse);

        // Mock GET file contents to return 404 (file does not exist)
        when(restTemplate.exchange(contains("/contents/"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // Mock PUT file contents response
        Map<String, Object> putBody = new HashMap<>();
        Map<String, Object> commitMap = new HashMap<>();
        commitMap.put("sha", "commit-sha-12345");
        putBody.put("commit", commitMap);
        ResponseEntity<Map> putResponse = new ResponseEntity<>(putBody, HttpStatus.OK);
        when(restTemplate.exchange(contains("/contents/"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(putResponse);

        githubCommitService.processCommitJob(new GithubCommitJobPayload(10L, 1L, 5L));

        assertEquals("commit-sha-12345", testSubmission.getCommitHash());
        verify(buildSubmissionRepository, times(1)).save(testSubmission);
    }
}
