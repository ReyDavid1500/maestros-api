package com.maestros.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateMaestroProfileRequest(
        @Size(max = 1000) String description,
        @Size(max = 20) String phone,
        Boolean isAvailable) {
}
