package com.famicup.modelo.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UsuarioResponse user) {
}
