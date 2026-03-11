package com.maestros.dto.response;

public record ChatMessageResponse(
        String id,
        String roomId,
        String senderId,
        String senderRole,
        String content,
        String createdAt,
        boolean read) {
}
