package com.maestros.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRatingRequest(
        @NotBlank String serviceRequestId,
        @NotNull @Min(1) @Max(5) Integer score,
        @Size(max = 500) String comment) {
}
