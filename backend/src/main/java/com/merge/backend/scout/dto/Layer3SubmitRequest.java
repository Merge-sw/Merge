package com.merge.backend.scout.dto;

import jakarta.validation.constraints.NotBlank;

public record Layer3SubmitRequest(
        @NotBlank String code
) {}
