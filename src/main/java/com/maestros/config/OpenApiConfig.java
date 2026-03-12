package com.maestros.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {

    /**
     * Global OpenAPI metadata + JWT security requirement.
     * The @SecurityRequirement at this level applies to all operations that
     * do NOT override it — combined with @SecurityRequirement on individual
     * controllers/methods this produces the padlock icon in Swagger UI.
     */
    @Bean
    public OpenAPI maestrosOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Maestros API")
                        .description("Backend para la plataforma Maestros de servicios del hogar")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    /**
     * "public" group — endpoints that require no JWT.
     * These map to the .permitAll() rules in SecurityConfig.
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("Public — No JWT required")
                .pathsToMatch(
                        "/api/v1/auth/**",
                        "/api/v1/categories/**",
                        "/api/v1/maestros",
                        "/api/v1/maestros/search",
                        "/api/v1/maestros/{id}",
                        "/api/v1/ratings/maestro/**",
                        "/actuator/**")
                .build();
    }

    /**
     * "protected" group — all regular authenticated endpoints.
     */
    @Bean
    public GroupedOpenApi protectedApi() {
        return GroupedOpenApi.builder()
                .group("protected")
                .displayName("Protected — JWT required")
                .pathsToMatch("/api/v1/**")
                .pathsToExclude(
                        "/api/v1/auth/**",
                        "/api/v1/categories/**",
                        "/api/v1/ratings/maestro/**")
                .build();
    }

    /**
     * "admin" group — reserved for future administration endpoints.
     * Empty now; add paths like "/api/v1/admin/**" when needed.
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Admin — Internal use")
                .pathsToMatch("/api/v1/admin/**")
                .build();
    }
}
