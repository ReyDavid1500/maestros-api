package com.maestros.config;

import io.lettuce.core.ClientOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${REDIS_HOST}")
    private String host;

    @Value("${REDIS_PORT}")
    private int port;

    @Value("${REDIS_PASSWORD}")
    private String password;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    /**
     * Explicit connection factory — causes Spring Boot's RedisAutoConfiguration
     * to back off via @ConditionalOnMissingBean(RedisConnectionFactory.class).
     *
     * SSL is driven by {@code spring.data.redis.ssl.enabled}, which is already
     * set to {@code false} in {@code application-dev.yml} and {@code true} in
     * the base {@code application.yml} (inherited by prod).
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(host, port);
        serverConfig.setPassword(password);

        LettuceClientConfiguration clientConfig;
        if (sslEnabled) {
            clientConfig = LettuceClientConfiguration.builder()
                    .useSsl()
                    .and()
                    .clientOptions(ClientOptions.builder().build())
                    .build();
        } else {
            clientConfig = LettuceClientConfiguration.defaultConfiguration();
        }

        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    /**
     * General-purpose template for typed key/value operations.
     * Both key and value serializers use plain UTF-8 strings so that
     * Redis CLI inspection and cross-service reads are straightforward.
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Convenience template for simple string key-value operations.
     * Used by {@code JwtService} for blacklist and refresh-token mappings.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
