package com.merge.backend.engagement.dto;

import java.time.Instant;

public record SeasonRequest(
        String name,
        Instant startDate,
        Instant endDate,
        boolean active
) {}
