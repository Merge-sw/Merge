package com.merge.backend.assessment.service;

import com.merge.backend.assessment.domain.Build;
import com.merge.backend.assessment.domain.BuildSubmission;
import com.merge.backend.assessment.dto.GithubCommitJobPayload;
import com.merge.backend.assessment.repository.BuildRepository;
import com.merge.backend.assessment.repository.BuildSubmissionRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.identity.service.TokenEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class GithubCommitServiceImpl implements GithubCommitService {

    private static final Logger log = LoggerFactory.getLogger(GithubCommitServiceImpl.class);

    private final BuildSubmissionRepository buildSubmissionRepository;
    private final BuildRepository buildRepository;
    private final StudentRepository studentRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final RestTemplate restTemplate;

    public GithubCommitServiceImpl(BuildSubmissionRepository buildSubmissionRepository,
                                   BuildRepository buildRepository,
                                   StudentRepository studentRepository,
                                   TokenEncryptionService tokenEncryptionService,
                                   RestTemplate restTemplate) {
        this.buildSubmissionRepository = buildSubmissionRepository;
        this.buildRepository = buildRepository;
        this.studentRepository = studentRepository;
        this.tokenEncryptionService = tokenEncryptionService;
        this.restTemplate = restTemplate;
    }

    @Override
    public void processCommitJob(GithubCommitJobPayload payload) throws Exception {
        log.info("[GithubCommit] Processing commit job for submissionId={}", payload.submissionId());

        BuildSubmission submission = buildSubmissionRepository.findById(payload.submissionId())
                .orElseThrow(() -> new IllegalArgumentException("Build submission not found: " + payload.submissionId()));

        Student student = submission.getStudent();
        if (student.getGithubOauthTokenEncrypted() == null) {
            log.warn("[GithubCommit] Student studentId={} has no GitHub OAuth token. Skipping push.", student.getId());
            return;
        }

        // 1. Decrypt token
        String decryptedToken = tokenEncryptionService.decrypt(student.getGithubOauthTokenEncrypted());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(decryptedToken);
        headers.setAccept(List.of(MediaType.valueOf("application/vnd.github+json")));
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        // 2. Fetch owner's username from GitHub
        String owner = null;
        try {
            ResponseEntity<Map> userResponse = executeWithRateLimitRetry(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            if (userResponse.getBody() != null) {
                owner = (String) userResponse.getBody().get("login");
            }
        } catch (Exception e) {
            log.error("[GithubCommit] Failed to retrieve GitHub user details for studentId={}", student.getId(), e);
            throw e;
        }

        if (owner == null || owner.isBlank()) {
            throw new IllegalStateException("Failed to determine GitHub username owner for studentId=" + student.getId());
        }

        String repo = student.getGithubPortfolioRepo() != null ? student.getGithubPortfolioRepo() : "merge-portfolio";
        String repoPath = repo.contains("/") ? repo : owner + "/" + repo;

        // Determine metadata
        String stage = "CADET";
        String concept = "Build";
        String type = "build";

        Build build = buildRepository.findById(payload.buildId()).orElse(null);
        if (build != null) {
            stage = build.getStageName();
        }

        // PUT path: contents/{stage}/{concept}/drill-{type}.js
        String contentsUrl = String.format("https://api.github.com/repos/%s/contents/%s/%s/drill-%s.js",
                repoPath, stage, concept, type);

        // 3. Fetch existing file to get its SHA (if any)
        String existingSha = null;
        try {
            ResponseEntity<Map> getResponse = executeWithRateLimitRetry(
                    contentsUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            if (getResponse.getStatusCode() == HttpStatus.OK && getResponse.getBody() != null) {
                existingSha = (String) getResponse.getBody().get("sha");
            }
        } catch (HttpClientErrorException.NotFound e) {
            // File does not exist yet
            log.debug("[GithubCommit] File not found at {}. Creating new file.", contentsUrl);
        } catch (Exception e) {
            log.warn("[GithubCommit] Error checking file existence at {}", contentsUrl, e);
        }

        // 4. PUT the file contents
        String commitMessage = String.format("Merge: %s — %s — Build — Passed", stage, concept);
        String base64Content = Base64.getEncoder().encodeToString(submission.getCode().getBytes(StandardCharsets.UTF_8));

        Map<String, Object> putBody = new HashMap<>();
        putBody.put("message", commitMessage);
        putBody.put("content", base64Content);
        if (existingSha != null) {
            putBody.put("sha", existingSha);
        }

        HttpEntity<Map<String, Object>> putEntity = new HttpEntity<>(putBody, headers);

        try {
            ResponseEntity<Map> putResponse = executeWithRateLimitRetry(
                    contentsUrl,
                    HttpMethod.PUT,
                    putEntity,
                    Map.class
            );

            Map responseBody = putResponse.getBody();
            if (responseBody != null && responseBody.containsKey("commit")) {
                Map commitMap = (Map) responseBody.get("commit");
                String commitHash = (String) commitMap.get("sha");
                submission.setCommitHash(commitHash);
                buildSubmissionRepository.save(submission);
                log.info("[GithubCommit] Successfully pushed commit to GitHub. commitHash={}", commitHash);
            }
        } catch (Exception e) {
            log.error("[GithubCommit] Failed to PUT file contents to GitHub for submissionId={}", payload.submissionId(), e);
            throw e;
        }
    }

    private <T> ResponseEntity<T> executeWithRateLimitRetry(String url, HttpMethod method, HttpEntity<?> entity, Class<T> responseType) throws Exception {
        int retries = 0;
        while (true) {
            try {
                return restTemplate.exchange(url, method, entity, responseType);
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (retries < 3) {
                    retries++;
                    log.warn("[GithubCommit] GitHub API rate limit (429) hit. Retrying in 60s (attempt {}/3)...", retries);
                    Thread.sleep(60000);
                } else {
                    throw e;
                }
            }
        }
    }
}
