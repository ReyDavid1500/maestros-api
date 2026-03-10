package com.maestros.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user,
        boolean isNewUser) {
}
