package com.merge.backend.identity.controller;

import com.merge.backend.identity.service.GitHubOAuthException;
import com.merge.backend.identity.service.GitHubOAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/students/me/github")
public class GitHubOAuthController {

    @Value("${github.oauth.client-id}")
    private String clientId;

    @Value("${github.oauth.redirect-uri}")
    private String redirectUri;

    private final GitHubOAuthService gitHubOAuthService;

    public GitHubOAuthController(GitHubOAuthService gitHubOAuthService) {
        this.gitHubOAuthService = gitHubOAuthService;
    }

    /**
     * ID-05: POST /api/v1/students/me/github/connect
     * Returns the GitHub OAuth authorization URL for the client to redirect to.
     * Requires valid JWT.
     */
    @PostMapping("/connect")
    public ResponseEntity<Map<String, String>> initiateConnect(
            @AuthenticationPrincipal UserDetails userDetails) {
        String authUrl = "https://github.com/login/oauth/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=repo"
                + "&state=" + userDetails.getUsername(); // state carries the student's email

        return ResponseEntity.ok(Map.of("authorizationUrl", authUrl));
    }

    /**
     * ID-05: GET /api/v1/students/me/github/callback
     * GitHub redirects here after user authorises.
     * Exchanges code for token, encrypts and stores it, creates portfolio repo.
     * Public endpoint — no JWT required (this is called by GitHub's redirect).
     */
    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam String code,
            @RequestParam String state) { // state = student email set in initiateConnect
        String portfolioUrl = gitHubOAuthService.connectGitHub(state, code);
        // Redirect the browser to the portfolio repo
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(portfolioUrl))
                .build();
    }

    // ── Exception handler ─────────────────────────────────────────────────────

    @ExceptionHandler(GitHubOAuthException.class)
    public ResponseEntity<Map<String, String>> handleGitHubError(GitHubOAuthException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", ex.getMessage()));
    }
}
