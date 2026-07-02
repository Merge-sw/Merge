package com.merge.backend.assessment.exception;

public class BuildComprehensionTimerExpiredException extends RuntimeException {

    public BuildComprehensionTimerExpiredException() {
        super("Build comprehension check server deadline has passed");
    }
}
