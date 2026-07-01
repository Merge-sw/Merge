package com.merge.backend.identity.service;

import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * ID-05: GitHub OAuth flow and portfolio repo creation.
 * Exchanges the authorization code for a token, encrypts it with AES-256,
 * stores it on the student record, and auto-creates the merge-portfolio repo.
 */
@Service
@Transactional
public class GitHubOAuthService {

    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String PORTFOLIO_REPO_NAME = "merge-portfolio";

    @Value("${github.oauth.client-id}")
    private String clientId;

    @Value("${github.oauth.client-secret}")
    private String clientSecret;

    private final StudentRepository studentRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final RestTemplate restTemplate;

    public GitHubOAuthService(StudentRepository studentRepository,
                              TokenEncryptionService tokenEncryptionService) {
        this.studentRepository = studentRepository;
        this.tokenEncryptionService = tokenEncryptionService;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Exchanges the OAuth code for an access token, encrypts and stores it,
     * then creates the merge-portfolio repo.
     *
     * @param studentEmail the authenticated student's email
     * @param code         the GitHub authorization code from the OAuth callback
     * @return the public URL of the created (or existing) portfolio repo
     */
    public String connectGitHub(String studentEmail, String code) {
        String accessToken = exchangeCodeForToken(code);

        // Encrypt the token with AES-256 before storage
        String encryptedToken = tokenEncryptionService.encrypt(accessToken);

        // Fetch GitHub username to construct repo URL
        String githubUsername = fetchGitHubUsername(accessToken);

        // Create the portfolio repo (idempotent — ignores 422 if it already exists)
        createPortfolioRepo(accessToken);

        String repoName = githubUsername + "/" + PORTFOLIO_REPO_NAME;

        Student student = studentRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentEmail));

        student.setGithubOauthTokenEncrypted(encryptedToken);
        student.setGithubPortfolioRepo(repoName);
        studentRepository.save(student);

        return "https://github.com/" + repoName;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(GITHUB_TOKEN_URL, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new GitHubOAuthException("Failed to exchange GitHub authorization code");
        }

        Object tokenObj = response.getBody().get("access_token");
        if (tokenObj == null) {
            Object error = response.getBody().get("error_description");
            throw new GitHubOAuthException("GitHub token exchange error: " + error);
        }

        return tokenObj.toString();
    }

    private String fetchGitHubUsername(String accessToken) {
        HttpHeaders headers = bearerHeaders(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                GITHUB_API_BASE + "/user", HttpMethod.GET, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new GitHubOAuthException("Failed to fetch GitHub user profile");
        }

        return response.getBody().get("login").toString();
    }

    private void createPortfolioRepo(String accessToken) {
        HttpHeaders headers = bearerHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> repoPayload = Map.of(
                "name", PORTFOLIO_REPO_NAME,
                "description", "Merge Engineering Formation Portfolio",
                "private", false,
                "auto_init", true
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(repoPayload, headers);

        try {
            restTemplate.postForEntity(GITHUB_API_BASE + "/user/repos", entity, Map.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 422 Unprocessable Entity means the repo already exists — that's fine
            if (e.getStatusCode().value() != 422) {
                throw new GitHubOAuthException("Failed to create GitHub portfolio repo: " + e.getMessage());
            }
        }
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        return headers;
    }
}
