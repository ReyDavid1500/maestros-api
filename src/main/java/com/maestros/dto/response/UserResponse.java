package com.maestros.dto.response;

public record UserResponse(
        String id,
        String name,
        String email,
        String photoUrl,
        String phone,
        String role,
        String createdAt,
        boolean hasMaestroProfile) {
}
