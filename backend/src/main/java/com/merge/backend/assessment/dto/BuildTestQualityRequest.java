package com.merge.backend.assessment.dto;

import java.util.List;

/** Gate 4 — AI-08: Test suite quality — coverage, edge cases, and requirement alignment. */
public record BuildTestQualityRequest(
        String code,
        String testSuite,
        List<String> requirements
) {}
