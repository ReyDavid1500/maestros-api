package com.maestros.dto.response;

public record ServiceRequestResponse(
        String id,
        UserResponse client,
        MaestroProfileResponse maestro,
        ServiceCategoryResponse serviceCategory,
        String description,
        AddressResponse address,
        String scheduledAt,
        String paymentMethod,
        String status,
        String acceptedAt,
        String startedAt,
        String completedAt,
        String cancelledAt,
        String createdAt) {
}
