package com.merge.backend.assessment.judge0;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "judge0")
public class Judge0Properties {

    private String baseUrl = "http://judge0:2358";
    private int pollIntervalMs = 500;
    private int maxPollAttempts = 20;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public int getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(int pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }

    public int getMaxPollAttempts() { return maxPollAttempts; }
    public void setMaxPollAttempts(int maxPollAttempts) { this.maxPollAttempts = maxPollAttempts; }
}
