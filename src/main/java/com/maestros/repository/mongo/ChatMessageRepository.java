package com.maestros.repository.mongo;

import com.maestros.model.mongo.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

    long countByRoomIdAndReadFalseAndSenderIdNot(String roomId, String senderId);

    List<ChatMessage> findTop1ByRoomIdOrderByCreatedAtDesc(String roomId);
}
