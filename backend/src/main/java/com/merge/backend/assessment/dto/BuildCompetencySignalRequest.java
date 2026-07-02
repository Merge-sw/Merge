package com.merge.backend.assessment.dto;

import java.util.List;

/** Gate 5 — AI-09: SFIA competency signal — does the submission demonstrate the target competencies? */
public record BuildCompetencySignalRequest(
        String code,
        String testSuite,
        String architectureDocument,
        List<String> sfiaCompetencies
) {}
