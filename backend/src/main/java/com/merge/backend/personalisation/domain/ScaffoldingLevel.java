package com.merge.backend.personalisation.domain;

public enum ScaffoldingLevel {
    /** Heavy guidance: many hints used, low comprehension scores, frequent failures. */
    HIGH,
    /** Moderate guidance: mixed performance across concepts. */
    MEDIUM,
    /** Minimal guidance: few hints, high comprehension scores, mostly passing. */
    LOW
}
