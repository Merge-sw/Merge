package com.merge.backend.curriculum.service;

public class StageNotFoundException extends RuntimeException {
    public StageNotFoundException(String message) {
        super(message);
    }
}
