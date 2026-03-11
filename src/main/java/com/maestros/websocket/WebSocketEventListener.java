package com.maestros.websocket;

import com.maestros.model.postgres.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private static final String ONLINE_KEY_PREFIX = "ws:online:";
    private static final Duration ONLINE_TTL = Duration.ofSeconds(35);

    private final StringRedisTemplate redis;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        String userId = extractUserId(StompHeaderAccessor.wrap(event.getMessage()));
        if (userId == null)
            return;

        redis.opsForValue().set(ONLINE_KEY_PREFIX + userId, "1", ONLINE_TTL);
        log.info("WebSocket connected: userId={}", userId);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String userId = extractUserId(StompHeaderAccessor.wrap(event.getMessage()));
        if (userId == null)
            return;

        redis.delete(ONLINE_KEY_PREFIX + userId);
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
