package com.maestros.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.maestros.exception.InvalidTokenException;
import com.maestros.model.postgres.User;
import com.maestros.repository.postgres.UserRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public WebSocketAuthInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || StompCommand.CONNECT != accessor.getCommand()) {
            // Only intercept CONNECT frames; let everything else through.
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new AccessDeniedException("Missing or malformed Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            DecodedJWT claims = jwtService.validateToken(token);

            UUID userId = UUID.fromString(claims.getClaim("userId").asString());
            String role = claims.getClaim("role").asString();

            User user = userRepository.findById(userId)
                    .filter(User::isActive)
                    .orElseThrow(() -> new AccessDeniedException("User not found or inactive"));

            UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));

            accessor.setUser(principal);

        } catch (InvalidTokenException e) {
            throw new AccessDeniedException("Invalid or expired token", e);
        }

        return message;
    }
}
