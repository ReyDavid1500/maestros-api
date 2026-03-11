package com.maestros.dto.response;

import java.util.List;

public record MaestroProfileResponse(
        String id,
        String userId,
        String name,
        String photoUrl,
        String description,
        List<MaestroServiceResponse> services,
        Double averageRating,
        Integer totalJobs,
        boolean isAvailable,
        boolean isVerified) {
}
