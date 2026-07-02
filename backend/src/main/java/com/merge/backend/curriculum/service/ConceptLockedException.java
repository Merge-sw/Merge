package com.merge.backend.curriculum.service;

public class ConceptLockedException extends RuntimeException {
    public ConceptLockedException(String message) {
        super(message);
    }
}
