package com.maestros.dto.response;

public record ChatRoomResponse(
        String roomId,
        String serviceRequestId,
        OtherParticipant otherParticipant,
        ChatMessageResponse lastMessage,
        long unreadCount) {

    public record OtherParticipant(String id, String name, String photoUrl) {
    }
}
