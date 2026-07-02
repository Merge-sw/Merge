package com.merge.backend.engagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload for the MOMENTUM_CALCULATION job. Carries the Monday week_start this run covers. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MomentumCalculationPayload {

    /** ISO-8601 date string of the Monday that starts the 7-day window just completed (e.g. "2026-06-29"). */
    private String weekStart;
}
