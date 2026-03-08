package com.ose.auth;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "请输入用户名") String username,
            @NotBlank(message = "请输入密码") String password
    ) {
    }

    public record UserProfile(Long id, String username, String displayName, String role) {
    }

    public record AuthResponse(String token, UserProfile user) {
    }
}
