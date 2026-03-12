package com.maestros.websocket;

import com.maestros.dto.request.SendMessagePayload;
import com.maestros.model.enums.SenderRole;
import com.maestros.model.mongo.ChatMessage;
import com.maestros.model.sql.ServiceRequest;
import com.maestros.repository.mongo.ChatMessageRepository;
import com.maestros.repository.sql.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private static final int MAX_CONTENT_LENGTH = 4000;
    private static final long RATE_LIMIT_MAX = 2;

    /** userId -> {windowStartSeconds, count} */
    private final ConcurrentHashMap<String, long[]> rateLimitMap = new ConcurrentHashMap<>();

    private final ChatMessageRepository chatMessageRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // -------------------------------------------------------------------------
    // /app/chat.send
    // -------------------------------------------------------------------------

    @MessageMapping("/chat.send")
    public void handleMessage(@Payload SendMessagePayload payload, Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        String roomId = payload.roomId();
        String content = payload.content();

        // 1. Validate roomId and membership
        RoomParts room = parseAndValidateRoom(roomId, userId);
        if (room == null) {
            log.warn("User [{}] attempted to send to unauthorized room [{}]", userId, roomId);
            return;
        }

        // 2. Sanitize and validate content
        if (content == null)
            return;
        String sanitized = Jsoup.clean(content, Safelist.none()).strip();
        if (sanitized.isBlank())
            return;
        if (sanitized.length() > MAX_CONTENT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_CONTENT_LENGTH);
        }

        // 3. Rate limiting — max 2 messages per second per user
        long nowSec = Instant.now().getEpochSecond();
        long[] window = rateLimitMap.compute(userId.toString(), (k, v) -> {
            if (v == null || v[0] != nowSec)
                return new long[] { nowSec, 1 };
            v[1]++;
            return v;
        });
        if (window[1] > RATE_LIMIT_MAX) {
            log.debug("Rate limit exceeded for user [{}]", userId);
            return;
        }

        // 4. Persist to MongoDB
        ChatMessage message = ChatMessage.builder()
                .roomId(roomId)
                .senderId(userId.toString())
                .senderRole(room.isClient ? SenderRole.CLIENT : SenderRole.MAESTRO)
                .content(sanitized)
                .createdAt(Instant.now())
                .build();
        message = chatMessageRepository.save(message);

        // 5. Broadcast to room topic
        Map<String, Object> response = buildMessageResponse(message);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, (Object) response);
    }

    // -------------------------------------------------------------------------
    // /app/chat.typing
    // -------------------------------------------------------------------------

    @MessageMapping("/chat.typing")
    public void handleTypingIndicator(@Payload Map<String, String> payload, Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        String roomId = payload.get("roomId");
        if (roomId == null)
            return;

        RoomParts room = parseAndValidateRoom(roomId, userId);
        if (room == null) {
            log.warn("User [{}] attempted typing indicator on unauthorized room [{}]", userId, roomId);
            return;
        }

        Map<String, Object> typingPayload = Map.of(
                "userId", userId.toString(),
                "isTyping", true);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId + "/typing", (Object) typingPayload);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Parses a roomId of the format {clientId}_{maestroId}_{serviceRequestId},
     * loads the ServiceRequest, and verifies the given userId is a participant.
     * Returns null if invalid or unauthorised.
     */
    private RoomParts parseAndValidateRoom(String roomId, UUID userId) {
        if (roomId == null)
            return null;
        String[] parts = roomId.split("_");
        if (parts.length != 3)
            return null;

        UUID serviceRequestId;
        try {
            serviceRequestId = UUID.fromString(parts[2]);
        } catch (IllegalArgumentException e) {
            return null;
        }

        ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId).orElse(null);
        if (sr == null)
            return null;

        boolean isClient = sr.getClient().getId().equals(userId);
        boolean isMaestro = sr.getMaestro().getId().equals(userId);
        if (!isClient && !isMaestro)
            return null;

        return new RoomParts(isClient);
    }

    private Map<String, Object> buildMessageResponse(ChatMessage msg) {
        return Map.of(
                "id", msg.getId(),
                "roomId", msg.getRoomId(),
                "senderId", msg.getSenderId(),
                "senderRole", msg.getSenderRole().name(),
                "content", msg.getContent(),
                "createdAt", msg.getCreatedAt().toString(),
                "read", msg.isRead());
    }

    private record RoomParts(boolean isClient) {
    }
}
