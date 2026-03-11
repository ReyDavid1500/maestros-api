package com.maestros.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maestros.dto.response.ApiResponse;
import com.maestros.repository.postgres.UserRepository;
import com.maestros.security.JwtAuthFilter;
import com.maestros.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Value("${ALLOWED_ORIGINS}")
        private String allowedOriginsRaw;

        private final JwtService jwtService;
        private final UserRepository userRepository;
        private final ObjectMapper objectMapper;

        public SecurityConfig(JwtService jwtService,
                        UserRepository userRepository,
                        ObjectMapper objectMapper) {
                this.jwtService = jwtService;
                this.userRepository = userRepository;
                this.objectMapper = objectMapper;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .formLogin(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints — no JWT required
                                                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/maestros").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/maestros/search").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/maestros/{id}").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/ratings/maestro/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/google").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                                                .requestMatchers("/ws/**").permitAll()
                                                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .requestMatchers("/actuator/health").permitAll()
                                                // Everything else requires a valid JWT
                                                .anyRequest().authenticated())
                                .addFilterBefore(
                                                new JwtAuthFilter(jwtService, userRepository),
                                                UsernamePasswordAuthenticationFilter.class)
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(401);
                                                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                                        response.setCharacterEncoding("UTF-8");
                                                        objectMapper.writeValue(
                                                                        response.getWriter(),
                                                                        ApiResponse.error("No autenticado"));
                                                })
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        response.setStatus(403);
                                                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                                        response.setCharacterEncoding("UTF-8");
                                                        objectMapper.writeValue(
                                                                        response.getWriter(),
                                                                        ApiResponse.error("Acceso denegado"));
                                                }));

                return http.build();
        }

        @Bean
        public UserDetailsService userDetailsService() {
                return username -> {
                        com.maestros.model.postgres.User user = userRepository.findByEmail(username)
                                        .orElseThrow(() -> new UsernameNotFoundException(
                                                        "Usuario no encontrado: " + username));
                        return org.springframework.security.core.userdetails.User.builder()
                                        .username(user.getEmail())
                                        .password("") // password not used — authentication is JWT-based
                                        .authorities("ROLE_" + user.getRole().name())
                                        .build();
                };
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                List<String> origins = Arrays.asList(allowedOriginsRaw.split(","));
                config.setAllowedOrigins(origins.stream().map(String::strip).toList());

                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setExposedHeaders(List.of(
                                "X-RateLimit-Limit",
                                "X-RateLimit-Remaining",
                                "X-RateLimit-Reset",
                                "Retry-After"));
                config.setAllowCredentials(false);
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
