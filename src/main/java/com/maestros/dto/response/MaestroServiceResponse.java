package com.maestros.dto.response;

public record MaestroServiceResponse(
        ServiceCategoryResponse serviceCategory,
        Long priceClp,
        String estimatedTime) {
}
