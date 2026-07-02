package com.merge.backend.assessment.dto;

public record BuildComprehensionSubmitResponse(boolean passed, Integer xpAwarded) {

    public static BuildComprehensionSubmitResponse failed() {
        return new BuildComprehensionSubmitResponse(false, null);
    }

    public static BuildComprehensionSubmitResponse passed(int xpAwarded) {
        return new BuildComprehensionSubmitResponse(true, xpAwarded);
    }
}
