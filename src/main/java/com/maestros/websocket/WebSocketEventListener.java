package com.maestros.websocket;

import com.maestros.model.sql.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private static final int MAX_SESSIONS = 3;

    /** userId → active session count */
    private final ConcurrentHashMap<String, AtomicInteger> sessionCounts = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = extractUserId(accessor);
        if (userId == null)
            return;

        AtomicInteger counter = sessionCounts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        if (count > MAX_SESSIONS) {
            counter.decrementAndGet();

            String sessionId = accessor.getSessionId();
            log.warn("event=WEBSOCKET_MAX_SESSIONS userId={} sessionId={} activeCount={}", userId, sessionId, count);

            if (sessionId != null) {
                SimpMessageHeaderAccessor notifyAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
                notifyAccessor.setSessionId(sessionId);
                notifyAccessor.setLeaveMutable(true);
                messagingTemplate.convertAndSendToUser(
                        sessionId,
                        "/queue/errors",
                        Map.of("error", "Número máximo de conexiones simultáneas alcanzado"),
                        notifyAccessor.getMessageHeaders());
            }
            return;
        }

        log.info("WebSocket connected: userId={} activeSessions={}", userId, count);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = extractUserId(accessor);
        if (userId == null)
            return;

        AtomicInteger counter = sessionCounts.get(userId);
        if (counter != null && counter.decrementAndGet() <= 0) {
            sessionCounts.remove(userId);
        }

        log.info("WebSocket disconnected: userId={}", userId);
    }

    private String extractUserId(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof User user) {
            return user.getId().toString();
        }
        return null;
    }
}
