package com.maestros.dto.response;

public record RatingResponse(
        String id,
        RaterInfo rater,
        Integer score,
        String comment,
        String createdAt) {

    public record RaterInfo(String id, String name, String photoUrl) {
    }
}
