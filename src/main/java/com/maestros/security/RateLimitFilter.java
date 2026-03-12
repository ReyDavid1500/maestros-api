package com.maestros.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.maestros.security.RateLimitConfig.*;
import static io.github.bucket4j.Bandwidth.builder;

/**
 * Rate limiting filter — runs after {@link JwtAuthFilter} so that the
 * SecurityContext already contains the authenticated user.
 * <p>
 * Redis bucket key format: {@code ratelimit:{GROUP}:{key}}
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Skip WebSocket upgrade requests — handled at transport level
        if (uri.startsWith("/ws") || uri.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Determine rate limit group
        String group = resolveGroup(uri, method);

        // 3. Resolve or create bucket for this key
        RateLimitRule rule = RULES.get(group);
        String bucketKey = "ratelimit:" + group + ":" + resolveKey(request, rule);

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> Bucket.builder()
                .addLimit(builder()
                        .capacity(rule.capacity())
                        .refillIntervally(rule.refillTokens(), Duration.ofSeconds(rule.refillDurationSeconds()))
                        .build())
                .build());

        // 4. Try to consume 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        long resetEpochSeconds = Instant.now().getEpochSecond()
                + TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());

        if (probe.isConsumed()) {
            // 5a. Allowed — add informational headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(rule.capacity()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.setHeader("X-RateLimit-Reset", String.valueOf(resetEpochSeconds));
            filterChain.doFilter(request, response);
        } else {
            // 5b. Rejected — 429
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
            String ip = extractIp(request);
            String userId = extractUserId();

            log.warn("event=RATE_LIMIT_EXCEEDED group={} key={} ip={} userId={} path={}",
                    group, bucketKey, ip, userId, uri);

            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("X-RateLimit-Limit", String.valueOf(rule.capacity()));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(resetEpochSeconds));
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

            Map<String, Object> body = Map.of(
                    "success", false,
                    "data", (Object) null,
                    "message", "Demasiadas solicitudes. Intenta nuevamente en " + retryAfterSeconds + " segundos.");
            objectMapper.writeValue(response.getWriter(), body);
        }
    }

    // -------------------------------------------------------------------------
    // Group resolution
    // -------------------------------------------------------------------------

    private String resolveGroup(String uri, String method) {
        // Specific endpoints take priority over generic catch-alls
        if ("POST".equalsIgnoreCase(method) && uri.equals("/api/v1/auth/google")) {
            return AUTH_GOOGLE;
        }
        if ("POST".equalsIgnoreCase(method) && uri.startsWith("/api/v1/service-requests")) {
            return CREATE_SERVICE_REQUEST;
        }
        if ("POST".equalsIgnoreCase(method) && uri.equals("/api/v1/ratings")) {
            return CREATE_RATING;
        }
        if ("POST".equalsIgnoreCase(method) && uri.startsWith("/api/v1/files")) {
            return FILE_UPLOAD;
        }

        // Generic fallback: authenticated users get a higher quota
        if (extractUserId() != null) {
            return AUTHENTICATED;
        }
        return PUBLIC_GET;
    }

    // -------------------------------------------------------------------------
    // Key helpers
    // -------------------------------------------------------------------------

    private String resolveKey(HttpServletRequest request, RateLimitRule rule) {
        if ("USER_ID".equals(rule.keyType())) {
            String userId = extractUserId();
            // Fall back to IP if no authenticated user (should be caught by Spring
            // Security,
            // but belt-and-suspenders)
            return userId != null ? userId : extractIp(request);
        }
        return extractIp(request);
    }

    /**
     * Returns the userId string if the request is authenticated, null otherwise.
     */
    private String extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof com.maestros.model.sql.User user) {
            return user.getId().toString();
        }
        return null;
    }

    /**
     * Returns the real client IP, favouring {@code X-Forwarded-For} when the
     * app is running behind Azure Application Gateway.
     */
    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For may be a comma-separated list; leftmost is the original
            // client
            return xff.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}
