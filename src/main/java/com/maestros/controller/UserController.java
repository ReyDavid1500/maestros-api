package com.maestros.controller;

import com.maestros.dto.request.UpdateFcmTokenRequest;
import com.maestros.dto.request.UpdateUserRequest;
import com.maestros.dto.response.ApiResponse;
import com.maestros.dto.response.UserResponse;
import com.maestros.model.sql.User;
import com.maestros.security.AuthHelper;
import com.maestros.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final AuthHelper authHelper;

    public UserController(UserService userService, AuthHelper authHelper) {
        this.userService = userService;
        this.authHelper = authHelper;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        User current = authHelper.getCurrentUser();
        UserResponse response = userService.getMyProfile(current.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @Valid @RequestBody UpdateUserRequest request) {
        User current = authHelper.getCurrentUser();
        UserResponse response = userService.updateMyProfile(current.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Perfil actualizado"));
    }

    @PutMapping("/me/fcm-token")
    public ResponseEntity<ApiResponse<Void>> updateFcmToken(
            @Valid @RequestBody UpdateFcmTokenRequest request) {
        User current = authHelper.getCurrentUser();
        userService.updateFcmToken(current.getId(), request.fcmToken());
        return ResponseEntity.ok(ApiResponse.ok(null, "Token FCM actualizado"));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deactivateMyAccount() {
        User current = authHelper.getCurrentUser();
        userService.deactivateMyAccount(current.getId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Cuenta desactivada exitosamente"));
    }
}
