package com.maestros.controller;

import com.maestros.dto.request.GoogleAuthRequest;
import com.maestros.dto.request.RefreshTokenRequest;
import com.maestros.dto.response.ApiResponse;
import com.maestros.dto.response.AuthResponse;
import com.maestros.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(
            @Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse authResponse = authService.loginWithGoogle(request.idToken());
        return ResponseEntity.ok(ApiResponse.ok(authResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.ok(null, "Sesión cerrada exitosamente"));
    }
}
