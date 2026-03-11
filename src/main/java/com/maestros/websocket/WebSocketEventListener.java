package com.maestros.websocket;

import com.maestros.model.postgres.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private static final String ONLINE_KEY_PREFIX = "ws:online:";
    private static final String SESSION_KEY_PREFIX = "ws:sessions:";
    private static final int MAX_SESSIONS = 3;
    private static final Duration ONLINE_TTL = Duration.ofSeconds(35);
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = extractUserId(accessor);
        if (userId == null)
            return;

        // Presence key — refreshed on each heartbeat reconnect
        redis.opsForValue().set(ONLINE_KEY_PREFIX + userId, "1", ONLINE_TTL);

        // Concurrent session counter
        String sessionKey = SESSION_KEY_PREFIX + userId;
        Long count = redis.opsForValue().increment(sessionKey);
        if (count == 1) {
            redis.expire(sessionKey, SESSION_TTL);
        }

        if (count != null && count > MAX_SESSIONS) {
            // Immediately decrement — this session is not counted
            redis.opsForValue().decrement(sessionKey);

            String sessionId = accessor.getSessionId();
            log.warn("event=WEBSOCKET_MAX_SESSIONS userId={} sessionId={} activeCount={}", userId, sessionId, count);

            // Notify the over-limit session so the client can react
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

        redis.delete(ONLINE_KEY_PREFIX + userId);

        // Decrement session counter; remove key when it reaches zero
        String sessionKey = SESSION_KEY_PREFIX + userId;
        Long remaining = redis.opsForValue().decrement(sessionKey);
        if (remaining != null && remaining <= 0) {
            redis.delete(sessionKey);
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
