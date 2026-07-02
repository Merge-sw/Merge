package com.merge.backend.curriculum.dto;

public record ResourceCompletionResponse(
        int xpAwarded,
        boolean alreadyCompleted
) {
    public static ResourceCompletionResponse firstCompletion(int xpAwarded) {
        return new ResourceCompletionResponse(xpAwarded, false);
    }

    public static ResourceCompletionResponse repeat() {
        return new ResourceCompletionResponse(0, true);
    }
}
