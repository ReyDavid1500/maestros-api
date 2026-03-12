package com.maestros.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.maestros.dto.response.AuthResponse;
import com.maestros.dto.response.UserResponse;
import com.maestros.exception.ForbiddenException;
import com.maestros.exception.InvalidTokenException;
import com.maestros.mapper.UserMapper;
import com.maestros.model.enums.UserRole;
import com.maestros.model.sql.User;
import com.maestros.repository.sql.UserRepository;
import com.maestros.security.GoogleTokenVerifier;
import com.maestros.security.GoogleUserInfo;
import com.maestros.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Autowired
    private HttpServletRequest httpRequest;

    public AuthService(GoogleTokenVerifier googleTokenVerifier,
            UserRepository userRepository,
            JwtService jwtService,
            UserMapper userMapper) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    public AuthResponse loginWithGoogle(String googleIdToken) {
        GoogleUserInfo info;
        try {
            info = googleTokenVerifier.verify(googleIdToken);
        } catch (Exception e) {
            log.warn("event=\"LOGIN_FAILED\" ip=\"{}\" userAgent=\"{}\" reason=\"invalid_google_token\"",
                    ip(), userAgent());
            throw e;
        }

        Optional<User> existing = userRepository.findByEmail(info.email());
        boolean isNewUser;
        User user;

        if (existing.isEmpty()) {
            String resolvedName = (info.name() != null && !info.name().isBlank())
                    ? info.name()
                    : info.email().split("@")[0];
            user = User.builder()
                    .name(resolvedName)
                    .email(info.email())
                    .photoUrl(info.pictureUrl())
                    .role(UserRole.CLIENT)
                    .build();
            user = userRepository.save(user);
            isNewUser = true;
        } else {
            user = existing.get();

            if (!user.isActive()) {
                throw new ForbiddenException("Cuenta desactivada");
            }

            boolean changed = false;
            if (info.name() != null && !info.name().isBlank()
                    && !Objects.equals(user.getName(), info.name())) {
                user.setName(info.name());
                changed = true;
            }
            if (!Objects.equals(user.getPhotoUrl(), info.pictureUrl())) {
                user.setPhotoUrl(info.pictureUrl());
                changed = true;
            }
            if (changed) {
                user = userRepository.save(user);
            }
            isNewUser = false;
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        UserResponse userResponse = userMapper.toUserResponse(user);

        log.info("event=\"LOGIN_SUCCESS\" userId=\"{}\" isNewUser={} ip=\"{}\"",
                user.getId(), isNewUser, ip());

        return new AuthResponse(accessToken, refreshToken, userResponse, isNewUser);
    }

    public AuthResponse refreshToken(String refreshToken) {
        DecodedJWT decoded = jwtService.validateToken(refreshToken);

        // Access tokens carry a "role" claim; refresh tokens do not.
        if (!decoded.getClaim("role").isNull()) {
            throw new InvalidTokenException("Provided token is not a refresh token");
        }

        String userId = decoded.getClaim("userId").asString();
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new InvalidTokenException("User not found for refresh token"));

        if (!user.isActive()) {
            throw new ForbiddenException("Cuenta desactivada");
        }

        // Rotation: revoke the current refresh token before issuing a new one
        jwtService.revokeToken(refreshToken);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        UserResponse userResponse = userMapper.toUserResponse(user);

        log.debug("event=\"TOKEN_REFRESH\" userId=\"{}\"", userId);

        return new AuthResponse(newAccessToken, newRefreshToken, userResponse, false);
    }

    public void logout(String accessToken) {
        String userId = jwtService.extractUserId(accessToken);
        jwtService.revokeToken(accessToken);

        log.info("event=\"LOGOUT\" userId=\"{}\"", userId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String ip() {
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return httpRequest.getRemoteAddr();
    }

    private String userAgent() {
        return httpRequest.getHeader("User-Agent");
    }
}
