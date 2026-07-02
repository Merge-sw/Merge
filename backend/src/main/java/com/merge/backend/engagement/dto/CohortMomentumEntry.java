package com.merge.backend.engagement.dto;

/** One student's entry on the cohort momentum strip. Full name is never included. */
public record CohortMomentumEntry(
        String initials,
        String state,
        int totalXp
) {}
