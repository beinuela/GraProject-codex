package com.campus.material.security;

import com.campus.material.common.BizException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    @Test
    void parseAccessTokenShouldContainExpectedClaims() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("12345678901234567890123456789012");
        properties.setAccessExpireMinutes(30);
        properties.setRefreshExpireDays(7);

        JwtTokenProvider provider = new JwtTokenProvider(properties);

        String token = provider.generateAccessToken(1L, "admin", "ADMIN");
        Claims claims = provider.parseToken(token);

        assertEquals("admin", claims.getSubject());
        assertEquals("access", claims.get("typ", String.class));
        assertEquals(1L, claims.get("uid", Number.class).longValue());
        assertEquals("ADMIN", claims.get("role", String.class));
        assertNotNull(claims.getExpiration());
    }

    @Test
    void generateTokenShouldFailWhenSecretMissing() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("  ");
        properties.setAccessExpireMinutes(30);
        properties.setRefreshExpireDays(7);

        JwtTokenProvider provider = new JwtTokenProvider(properties);

        BizException ex = assertThrows(BizException.class, () ->
                provider.generateAccessToken(1L, "admin", "ADMIN"));
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void generateTokenShouldFailWhenSecretTooShort() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("short-secret");
        properties.setAccessExpireMinutes(30);
        properties.setRefreshExpireDays(7);

        JwtTokenProvider provider = new JwtTokenProvider(properties);

        BizException ex = assertThrows(BizException.class, () ->
                provider.generateRefreshToken(1L, "admin", "ADMIN", null));
        assertTrue(ex.getMessage().contains("32"));
    }
}
