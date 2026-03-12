package com.maestros.controller;

import com.maestros.dto.response.ApiResponse;
import com.maestros.dto.response.ChatMessageResponse;
import com.maestros.dto.response.ChatRoomResponse;
import com.maestros.model.sql.User;
import com.maestros.security.AuthHelper;
import com.maestros.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AuthHelper authHelper;

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRooms() {
        User current = authHelper.getCurrentUser();
        List<ChatRoomResponse> rooms = chatService.getChatRooms(current.getId());
        return ResponseEntity.ok(ApiResponse.ok(rooms));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        User current = authHelper.getCurrentUser();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<ChatMessageResponse> result = chatService.getChatMessages(roomId, current.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/rooms/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String roomId) {
        User current = authHelper.getCurrentUser();
        chatService.markAsRead(roomId, current.getId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Mensajes marcados como leídos"));
    }
}
