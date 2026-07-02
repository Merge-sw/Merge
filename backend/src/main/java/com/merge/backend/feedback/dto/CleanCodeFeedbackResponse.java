package com.merge.backend.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CleanCodeFeedbackResponse(
        boolean generating,
        Integer overallScore,
        List<String> namingIssues,
        List<String> functionSizeIssues,
        List<String> redundancyIssues,
        List<String> solidIssues,
        Instant generatedAt
) {
    public static CleanCodeFeedbackResponse emptyGenerating() {
        return new CleanCodeFeedbackResponse(true, null, null, null, null, null, null);
    }
}
