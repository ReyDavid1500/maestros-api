package com.maestros.service;

import com.maestros.dto.response.ChatMessageResponse;
import com.maestros.dto.response.ChatRoomResponse;
import com.maestros.exception.ForbiddenException;
import com.maestros.exception.ResourceNotFoundException;
import com.maestros.model.mongo.ChatMessage;
import com.maestros.model.sql.ServiceRequest;
import com.maestros.model.sql.User;
import com.maestros.repository.mongo.ChatMessageRepository;
import com.maestros.repository.sql.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final MongoTemplate mongoTemplate;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns all chat rooms for the given user, sorted by last message date desc.
     *
     * Uses a MongoDB aggregation pipeline to obtain each room's last message and
     * unread count in a single query:
     *
     * 1. $match — filter documents whose roomId is in the user's room list
     * 2. $sort — { createdAt: -1 } so that $first picks the newest message
     * 3. $group — group by roomId:
     * lastMessage : { $first: "$$ROOT" }
     * unreadCount : { $sum: { $cond: [ { $and: [
     * { $eq: ["$read", false] },
     * { $ne: ["$senderId", userId] }
     * ] }, 1, 0 ] } }
     * 4. $sort — { "lastMessage.createdAt": -1 } (rooms ordered by recency)
     */
    public List<ChatRoomResponse> getChatRooms(UUID userId) {
        // 1. Load all service requests where the user is client or maestro
        List<ServiceRequest> requests = serviceRequestRepository.findByClientIdOrMaestroId(userId, userId);

        if (requests.isEmpty())
            return Collections.emptyList();

        // 2. Build a map of roomId → ServiceRequest for later resolution
        Map<String, ServiceRequest> roomToRequest = new LinkedHashMap<>();
        for (ServiceRequest sr : requests) {
            String roomId = buildRoomId(sr);
            roomToRequest.put(roomId, sr);
        }

        List<String> roomIds = new ArrayList<>(roomToRequest.keySet());
        String userIdStr = userId.toString();

        // 3. MongoDB aggregation — single round-trip
        // Pipeline documented in method Javadoc above
        org.springframework.data.mongodb.core.aggregation.Aggregation agg = org.springframework.data.mongodb.core.aggregation.Aggregation
                .newAggregation(
                        org.springframework.data.mongodb.core.aggregation.Aggregation
                                .match(Criteria.where("roomId").in(roomIds)),
                        org.springframework.data.mongodb.core.aggregation.Aggregation
                                .sort(org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt")),
                        org.springframework.data.mongodb.core.aggregation.Aggregation
                                .group("roomId")
                                .first(org.springframework.data.mongodb.core.aggregation.Aggregation.ROOT)
                                .as("lastMessage")
                                .sum(org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                                        .when(Criteria.where("read").is(false)
                                                .and("senderId").ne(userIdStr))
                                        .then(1)
                                        .otherwise(0))
                                .as("unreadCount"),
                        org.springframework.data.mongodb.core.aggregation.Aggregation
                                .sort(org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.DESC,
                                        "lastMessage.createdAt")));

        List<Document> results = mongoTemplate
                .aggregate(agg, "chat_messages", Document.class)
                .getMappedResults();

        // 4. Map aggregation results to ChatRoomResponse
        List<ChatRoomResponse> rooms = new ArrayList<>();
        for (Document doc : results) {
            String roomId = doc.getString("_id");
            ServiceRequest sr = roomToRequest.get(roomId);
            if (sr == null)
                continue;

            Document lastMsgDoc = doc.get("lastMessage", Document.class);
            ChatMessageResponse lastMessage = lastMsgDoc != null ? docToResponse(lastMsgDoc) : null;
            long unreadCount = doc.get("unreadCount", Number.class).longValue();

            User otherUser = sr.getClient().getId().equals(userId)
                    ? sr.getMaestro()
                    : sr.getClient();

            rooms.add(new ChatRoomResponse(
                    roomId,
                    sr.getId().toString(),
                    new ChatRoomResponse.OtherParticipant(
                            otherUser.getId().toString(),
                            otherUser.getName(),
                            otherUser.getPhotoUrl()),
                    lastMessage,
                    unreadCount));
        }

        return rooms;
    }

    /**
     * Returns paginated message history for a room. Validates that the requesting
     * user is a participant before returning data.
     */
    public Page<ChatMessageResponse> getChatMessages(String roomId, UUID userId, Pageable pageable) {
        validateRoomAccess(roomId, userId);

        Page<ChatMessage> page = chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);

        List<ChatMessageResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    /**
     * Marks all messages from the other participant in the room as read.
     * Uses MongoTemplate updateMulti for a single database round-trip.
     */
    public void markAsRead(String roomId, UUID userId) {
        validateRoomAccess(roomId, userId);

        Query query = Query.query(
                Criteria.where("roomId").is(roomId)
                        .and("senderId").ne(userId.toString())
                        .and("read").is(false));

        Update update = Update.update("read", true);
        mongoTemplate.updateMulti(query, update, ChatMessage.class);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String buildRoomId(ServiceRequest sr) {
        return sr.getClient().getId() + "_" + sr.getMaestro().getId() + "_" + sr.getId();
    }

    /**
     * Parses roomId ({clientId}_{maestroId}_{serviceRequestId}), loads the
     * ServiceRequest, and verifies the given userId is a participant.
     * Throws ForbiddenException if not authorised.
     */
    private ServiceRequest validateRoomAccess(String roomId, UUID userId) {
        if (roomId == null || roomId.split("_").length != 3) {
            throw new ForbiddenException("Invalid room ID");
        }
        String[] parts = roomId.split("_");
        UUID serviceRequestId;
        try {
            serviceRequestId = UUID.fromString(parts[2]);
        } catch (IllegalArgumentException e) {
            throw new ForbiddenException("Invalid room ID");
        }

        ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found"));

        boolean isParticipant = sr.getClient().getId().equals(userId)
                || sr.getMaestro().getId().equals(userId);
        if (!isParticipant) {
            throw new ForbiddenException("Access denied to this chat room");
        }
        return sr;
    }

    private ChatMessageResponse toResponse(ChatMessage msg) {
        return new ChatMessageResponse(
                msg.getId(),
                msg.getRoomId(),
                msg.getSenderId(),
                msg.getSenderRole().name(),
                msg.getContent(),
                msg.getCreatedAt().toString(),
                msg.isRead());
    }

    private ChatMessageResponse docToResponse(Document doc) {
        return new ChatMessageResponse(
                doc.getObjectId("_id") != null ? doc.getObjectId("_id").toHexString()
                        : doc.getString("_id"),
                doc.getString("roomId"),
                doc.getString("senderId"),
                doc.getString("senderRole"),
                doc.getString("content"),
                doc.get("createdAt") != null ? doc.get("createdAt").toString() : null,
                Boolean.TRUE.equals(doc.getBoolean("read")));
    }
}
