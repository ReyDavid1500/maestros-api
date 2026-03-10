package com.maestros.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds hardened HTTP security headers to every response.
 *
 * <p>
 * Strict-Transport-Security is only emitted when the {@code prod} profile
 * is active — local and dev environments should not receive HSTS preloading.
 * </p>
 */
@Component
public class SecurityHeadersConfig extends OncePerRequestFilter {

    private static final String HSTS_VALUE = "max-age=31536000; includeSubDomains";

    private final boolean isProd;

    public SecurityHeadersConfig(Environment environment) {
        this.isProd = environment.acceptsProfiles(Profiles.of("prod"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Content-Security-Policy", "default-src 'none'");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");

        if (isProd) {
            response.setHeader("Strict-Transport-Security", HSTS_VALUE);
        }

        filterChain.doFilter(request, response);
    }
}
