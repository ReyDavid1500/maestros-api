package com.maestros.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MaestroServiceRequest(
        @NotBlank String serviceCategoryId,
        @NotNull @Min(1) @Max(10_000_000) Long priceClp,
        @NotBlank @Size(max = 50) String estimatedTime) {
}
