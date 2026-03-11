package com.maestros.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateServiceRequestRequest(
        @NotBlank String maestroId,
        @NotBlank String serviceCategoryId,
        @NotBlank @Size(max = 1000) String description,
        @NotBlank @Size(max = 200) String addressStreet,
        @NotBlank @Size(max = 20) String addressNumber,
        @NotBlank @Size(max = 100) String addressCity,
        @Size(max = 500) String addressInstructions,
        @NotBlank String scheduledAt) {
}
