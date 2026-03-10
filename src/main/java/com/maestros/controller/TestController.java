package com.maestros.controller;

import com.maestros.dto.response.ApiResponse;
import com.maestros.security.AuthHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    private final AuthHelper authHelper;

    public TestController(AuthHelper authHelper) {
        this.authHelper = authHelper;
    }

    @GetMapping("/protected")
    public ResponseEntity<ApiResponse<String>> protectedEndpoint() {
        String email = authHelper.getCurrentUser().getEmail();
        return ResponseEntity.ok(ApiResponse.ok("Token válido, usuario: " + email));
    }
}
