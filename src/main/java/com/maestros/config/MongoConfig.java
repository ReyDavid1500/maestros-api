package com.maestros.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.List;

/**
 * Explicit MongoDB configuration. Restricts repository scanning to the mongo
 * repository
 * package so Spring Data never tries to create Mongo repositories for JPA
 * interfaces.
 *
 * Auto-index creation (honoring @CompoundIndex / @Indexed on documents like
 * ChatMessage)
 * is enabled via spring.data.mongodb.auto-index-creation=true in the dev
 * profile.
 *
 * Spring Data MongoDB serialises enums as their name() String by default.
 * MongoCustomConversions is declared explicitly so it can be extended with
 * custom
 * converters in the future without changes to auto-configuration behaviour.
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.maestros.repository.mongo")
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of());
    }
}
