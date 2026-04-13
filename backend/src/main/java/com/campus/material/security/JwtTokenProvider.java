package com.campus.material.security;

import com.campus.material.common.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    private SecretKey secretKey() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new BizException(500, "JWT_SECRET 未配置，请在环境变量中设置");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new BizException(500, "JWT_SECRET 长度不足，至少需要 32 字节");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String username, String roleCode) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getAccessExpireMinutes() * 60L);
        return Jwts.builder()
                .setSubject(username)
                .claim("uid", userId)
                .claim("role", roleCode)
                .claim("typ", "access")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expireAt))
                .signWith(secretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(Long userId, String username, String roleCode, String jti) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getRefreshExpireDays() * 24L * 60L * 60L);
        return Jwts.builder()
                .setSubject(username)
                .claim("uid", userId)
                .claim("role", roleCode)
                .claim("typ", "refresh")
                .setId(jti == null || jti.isBlank() ? UUID.randomUUID().toString() : jti)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expireAt))
                .signWith(secretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
