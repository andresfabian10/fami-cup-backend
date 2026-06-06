package com.famicup.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import com.famicup.configuracion.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(new JwtProperties(
            "test-secret-key-for-famicup-jwt-tests-with-more-than-64-chars",
            "famicup-test",
            900,
            3600,
            1800));

    @Test
    void generatesAndValidatesAccessToken() {
        UserDetails user = User.withUsername("tia.maria").password("hash").roles("PLAYER").build();

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("tia.maria");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
        assertThat(token).isNotBlank();
    }

    @Test
    void hashesRefreshTokenWithoutReturningOriginalToken() {
        String refreshToken = jwtService.generateRefreshToken();

        String hash = jwtService.hashToken(refreshToken);

        assertThat(hash).isNotBlank();
        assertThat(hash).isNotEqualTo(refreshToken);
        assertThat(jwtService.hashToken(refreshToken)).isEqualTo(hash);
    }
}
