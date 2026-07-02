package com.merge.backend.assessment.service;

import com.merge.backend.assessment.dto.GithubCommitJobPayload;

public interface GithubCommitService {
    void processCommitJob(GithubCommitJobPayload payload) throws Exception;
}
