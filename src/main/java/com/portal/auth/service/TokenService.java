package com.portal.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final StringRedisTemplate redis;

    private static final String REFRESH_KEY = "refresh:";

    public TokenService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${app.jwt.refresh-token-expiration}") long refreshTokenExpiration,
            StringRedisTemplate redis) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.redis = redis;
    }

    public String generateAccessToken(String userId, int tokenVersion, String deviceId) {
        var now = new Date();
        return Jwts.builder()
                .subject(userId)
                .claim("tokenVersion", tokenVersion)
                .claim("deviceId", deviceId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        var refreshToken = UUID.randomUUID().toString();
        redis.opsForValue().set(
                REFRESH_KEY + refreshToken,
                userId,
                refreshTokenExpiration,
                TimeUnit.MILLISECONDS);
        return refreshToken;
    }

    public String refreshAccessToken(String refreshToken) {
        String userId = redis.opsForValue().getAndDelete(REFRESH_KEY + refreshToken);
        if (userId == null) {
            throw new com.portal.auth.exception.AuthException("AUTH_008", "Invalid or expired refresh token");
        }
        return generateRefreshToken(userId);
    }

    public void revokeRefreshToken(String refreshToken) {
        redis.delete(REFRESH_KEY + refreshToken);
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
}
