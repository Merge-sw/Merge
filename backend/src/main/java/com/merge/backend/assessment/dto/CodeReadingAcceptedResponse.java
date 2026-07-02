package com.merge.backend.assessment.dto;

public record CodeReadingAcceptedResponse(boolean accepted) {
    public static CodeReadingAcceptedResponse ok() {
        return new CodeReadingAcceptedResponse(true);
    }
}
