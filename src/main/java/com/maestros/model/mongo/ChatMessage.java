package com.maestros.model.mongo;

import com.maestros.model.enums.SenderRole;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "chat_messages")
@CompoundIndexes({
        @CompoundIndex(name = "idx_room_created", def = "{'roomId': 1, 'createdAt': 1}"),
        @CompoundIndex(name = "idx_room_read", def = "{'roomId': 1, 'read': 1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private String roomId;

    private String senderId;

    private SenderRole senderRole;

    private String content;

    private Instant createdAt;

    @Builder.Default
    private boolean read = false;
}
