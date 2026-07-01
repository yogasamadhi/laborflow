package com.company.labor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtTokenProvider(@Value("${laborflow.jwt.secret}") String secret, @Value("${laborflow.jwt.expiration-minutes}") long expirationMinutes) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = (secret + "00000000000000000000000000000000").substring(0, 32).getBytes(StandardCharsets.UTF_8);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMinutes = expirationMinutes;
    }

    public JwtToken createLoginToken(LoginUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationMinutes * 60);
        String value = Jwts.builder()
                .subject(user.getUser().getId().toString())
                .claim("username", user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
        return new JwtToken(value, expiresAt);
    }

    public String createToken(LoginUser user) {
        return createLoginToken(user).value();
    }

    public Long parseUserId(String token) {
        Claims claims = parseClaims(token);
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new JwtAuthenticationException("登录凭证无效，请重新登录", ex);
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException ex) {
            throw new JwtAuthenticationException("登录已过期，请重新登录", ex);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtAuthenticationException("登录凭证无效，请重新登录", ex);
        }
    }

    public record JwtToken(String value, Instant expiresAt) {
    }
}
