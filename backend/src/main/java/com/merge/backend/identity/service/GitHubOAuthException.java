package com.merge.backend.identity.service;

public class GitHubOAuthException extends RuntimeException {
    public GitHubOAuthException(String message) {
        super(message);
    }
}
