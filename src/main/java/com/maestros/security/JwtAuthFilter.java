package com.maestros.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.maestros.exception.InvalidTokenException;
import com.maestros.model.postgres.User;
import com.maestros.repository.postgres.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            DecodedJWT claims = jwtService.validateToken(token);

            UUID userId = UUID.fromString(claims.getClaim("userId").asString());
            String role = claims.getClaim("role").asString();

            User user = userRepository.findById(userId).orElse(null);

            if (user != null && user.isActive()) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                MDC.put("userId", userId.toString());
                MDC.put("ip", getClientIp(request));
            }

        } catch (InvalidTokenException ignored) {
            // Invalid token — leave SecurityContext unauthenticated.
            // Spring Security will return 401 for protected endpoints automatically.
        } finally {
            filterChain.doFilter(request, response);
            MDC.remove("userId");
            MDC.remove("ip");
        }
    }

    /**
     * Extracts the real client IP, respecting common reverse-proxy headers.
     * Falls back to the direct remote address when no proxy header is present.
     */
    private static String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may contain a comma-separated list; the first entry is the
            // originating IP.
            return forwarded.split(",")[0].strip();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.strip();
        }
        return request.getRemoteAddr();
    }
}
