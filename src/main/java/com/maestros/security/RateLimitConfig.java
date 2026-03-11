package com.maestros.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.Map;

@Configuration
public class RateLimitConfig {

    /**
     * Token-bucket rule for a group of endpoints.
     * <p>
     * {@code capacity} – max tokens at any point in time
     * {@code refillTokens} – tokens added per period
     * {@code refillDurationSeconds}– period length in seconds
     * {@code keyType} – "IP" or "USER_ID"
     */
    public record RateLimitRule(long capacity, long refillTokens, long refillDurationSeconds, String keyType) {
    }

    /** Endpoint group names used as part of the Redis bucket key. */
    public static final String AUTH_GOOGLE = "AUTH_GOOGLE";
    public static final String PUBLIC_GET = "PUBLIC_GET";
    public static final String AUTHENTICATED = "AUTHENTICATED";
    public static final String CREATE_SERVICE_REQUEST = "CREATE_SERVICE_REQUEST";
    public static final String CREATE_RATING = "CREATE_RATING";
    public static final String FILE_UPLOAD = "FILE_UPLOAD";
    public static final String WEBSOCKET_CONNECT = "WEBSOCKET_CONNECT";

    /**
     * Immutable map of group → rule, consumed by {@link RateLimitFilter}.
     *
     * <pre>
     * Group                  Capacity  Refill  Period      Key
     * AUTH_GOOGLE            10        10      900 s       IP
     * PUBLIC_GET             60        60      60 s        IP
     * AUTHENTICATED          120       120     60 s        USER_ID
     * CREATE_SERVICE_REQUEST 5         5       600 s       USER_ID
     * CREATE_RATING          3         3       3600 s      USER_ID
     * FILE_UPLOAD            10        10      3600 s      USER_ID
     * WEBSOCKET_CONNECT      3         3       86400 s     USER_ID  (concurrent sessions)
     * </pre>
     */
    public static final Map<String, RateLimitRule> RULES = Map.of(
            AUTH_GOOGLE, new RateLimitRule(10, 10, 900, "IP"),
            PUBLIC_GET, new RateLimitRule(60, 60, 60, "IP"),
            AUTHENTICATED, new RateLimitRule(120, 120, 60, "USER_ID"),
            CREATE_SERVICE_REQUEST, new RateLimitRule(5, 5, 600, "USER_ID"),
            CREATE_RATING, new RateLimitRule(3, 3, 3600, "USER_ID"),
            FILE_UPLOAD, new RateLimitRule(10, 10, 3600, "USER_ID"),
            WEBSOCKET_CONNECT, new RateLimitRule(3, 3, 86400, "USER_ID"));

    /**
     * CAS-based Bucket4j ProxyManager backed by the existing Lettuce connection.
     * <p>
     * All application nodes share the same counters in Redis, making the rate
     * limiting accurate across horizontal scaling.
     */
    @Bean
    public ProxyManager<String> bucketProxyManager(RedisConnectionFactory connectionFactory) {
        LettuceConnectionFactory factory = (LettuceConnectionFactory) connectionFactory;
        RedisClient client = (RedisClient) factory.getNativeClient();
        StatefulRedisConnection<String, byte[]> connection = client
                .connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        return LettuceBasedProxyManager.builderFor(connection).build();
    }

    @Bean
    public RateLimitFilter rateLimitFilter(ProxyManager<String> bucketProxyManager,
            ObjectMapper objectMapper) {
        return new RateLimitFilter(bucketProxyManager, objectMapper);
    }
}
