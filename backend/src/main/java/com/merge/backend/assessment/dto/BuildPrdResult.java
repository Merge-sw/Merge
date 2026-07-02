package com.merge.backend.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Structured output from AI-03 (BuildPrdWriter). Written directly to builds.*. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildPrdResult {

    /** Full PRD prose — personalised to the student's gaps and patterns. */
    private String prd;

    /**
     * Hidden test suite generated alongside the PRD.
     * Used exclusively for Gate 1 (JUDGE0) evaluation; never returned via any API endpoint.
     */
    private String hiddenTestSuite;

    /** Ordered functional requirements extracted from the PRD. */
    private List<String> requirements;

    /** Non-functional and technical constraints the student must respect. */
    private List<String> constraints;

    /** SFIA competency codes this build challenge is designed to assess. */
    private List<String> sfiaCompetencies;
}
