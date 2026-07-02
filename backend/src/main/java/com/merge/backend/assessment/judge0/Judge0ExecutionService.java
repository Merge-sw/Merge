package com.merge.backend.assessment.judge0;

import org.springframework.stereotype.Service;

@Service
public class Judge0ExecutionService {

    private final Judge0Client client;

    public Judge0ExecutionService(Judge0Client client) {
        this.client = client;
    }

    /**
     * Concatenates the student's solution and test suite into a single JavaScript
     * program, sends it to Judge0, and returns the execution outcome.
     *
     * @param code      student's solution code
     * @param testSuite student-authored tests that exercise the solution
     */
    public Judge0Outcome execute(String code, String testSuite) {
        String combined = code + "\n\n" + testSuite;
        return client.execute(combined);
    }
}
