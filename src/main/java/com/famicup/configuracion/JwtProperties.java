package com.famicup.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessTokenExpirationSeconds,
        long refreshTokenExpirationSeconds,
        long refreshInactivityExpirationSeconds) {
}
