package com.campus.material.modules.auth;

import com.campus.material.common.ApiResponse;
import com.campus.material.modules.auth.dto.LoginRequest;
import com.campus.material.modules.auth.dto.LoginResponse;
import com.campus.material.modules.auth.dto.RefreshTokenRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request.getRefreshToken()));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        return ApiResponse.ok(authService.me());
    }

    @GetMapping("/menus")
    public ApiResponse<List<Map<String, String>>> menus() {
        return ApiResponse.ok(authService.menus());
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logout() {
        authService.logoutCurrentUser();
        return ApiResponse.ok(null);
    }

    @GetMapping("/token-policy")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> tokenPolicy() {
        return ApiResponse.ok(authService.tokenPolicyOverview());
    }

    @PostMapping("/token-cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> tokenCleanup() {
        return ApiResponse.ok(authService.triggerTokenCleanup());
    }
}
