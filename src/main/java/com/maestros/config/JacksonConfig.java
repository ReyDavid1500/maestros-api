package com.maestros.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    /**
     * Explicit ObjectMapper bean.
     *
     * JavaTimeModule is registered so that Java 8+ date/time types (Instant,
     * LocalDateTime, etc.) serialise to ISO-8601 strings instead of raw numeric
     * timestamps.
     *
     * WRITE_DATES_AS_TIMESTAMPS is disabled so Instant fields appear as
     * "2026-03-10T12:00:00Z" in JSON rather than [seconds, nanos] arrays.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
