package com.maestros.dto.response;

import java.util.List;

public record MaestroListItemResponse(
        String id,
        String userId,
        String name,
        String photoUrl,
        String descriptionSnippet,
        List<MaestroServiceResponse> services,
        Double averageRating,
        Integer totalJobs,
        boolean isAvailable,
        boolean isVerified) {
}
