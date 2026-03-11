package com.maestros.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateMaestroProfileRequest(
        @NotBlank @Size(max = 1000) String description,
        @NotEmpty @Valid List<MaestroServiceRequest> services,
        @NotBlank @Size(max = 20) String phone) {
}
