package com.merge.backend.assessment.judge0;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Judge0SubmissionRequest {

    @JsonProperty("source_code")
    private String sourceCode;

    @JsonProperty("language_id")
    private int languageId;

    @JsonProperty("cpu_time_limit")
    private int cpuTimeLimit;

    @JsonProperty("memory_limit")
    private int memoryLimit;

    @JsonProperty("enable_network")
    private boolean enableNetwork;

    public Judge0SubmissionRequest(String sourceCode) {
        this.sourceCode = sourceCode;
        this.languageId = 93;
        this.cpuTimeLimit = 2;
        this.memoryLimit = 262144;
        this.enableNetwork = false;
    }

    public String getSourceCode() { return sourceCode; }
    public int getLanguageId() { return languageId; }
    public int getCpuTimeLimit() { return cpuTimeLimit; }
    public int getMemoryLimit() { return memoryLimit; }
    public boolean isEnableNetwork() { return enableNetwork; }
}
