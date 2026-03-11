package com.maestros.dto.response;

import java.util.List;

public record RatingsPageResponse(
        List<RatingResponse> ratings,
        Double averageScore,
        Long totalRatings,
        int page,
        int size,
        int totalPages) {
}
