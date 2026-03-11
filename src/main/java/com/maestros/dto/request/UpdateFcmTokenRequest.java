package com.maestros.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFcmTokenRequest(
        @NotBlank @Size(max = 500) String fcmToken) {
}
