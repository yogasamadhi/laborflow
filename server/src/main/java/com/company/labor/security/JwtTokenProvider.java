package com.company.labor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    /**
     * HS256 requires at least 256 bits (32 bytes) of key material. Anything shorter
     * is cryptographically weak, so we refuse to boot instead of silently padding —
     * the previous behaviour appended '0' characters which effectively neutralized
     * any short secret.
     */
    static final int MIN_SECRET_BYTES = 32;
    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtTokenProvider(@Value("${laborflow.jwt.secret}") String secret,
                            @Value("${laborflow.jwt.expiration-minutes}") long expirationMinutes) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "laborflow.jwt.secret is not configured. Set JWT_SECRET to a random string of at least "
                            + MIN_SECRET_BYTES + " bytes.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "laborflow.jwt.secret is too short: " + keyBytes.length + " bytes, need at least "
                            + MIN_SECRET_BYTES + ". Set JWT_SECRET to a longer random value.");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMinutes = expirationMinutes;
    }

    public JwtToken createLoginToken(LoginUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationMinutes * 60);
        String jti = UUID.randomUUID().toString();
        String value = Jwts.builder()
                .id(jti)
                .subject(user.getUser().getId().toString())
                .claim("username", user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
        return new JwtToken(value, jti, expiresAt);
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

    /**
     * Returns the parsed token claims, including the {@code jti} (token id) used by
     * the blacklist check during logout.
     */
    public ParsedToken parse(String token) {
        Claims claims = parseClaims(token);
        String jti = claims.getId();
        if (jti == null || jti.isBlank()) {
            // Tokens issued before the JTI migration lack an id and cannot be revoked.
            throw new JwtAuthenticationException("登录凭证无效，请重新登录", null);
        }
        return new ParsedToken(jti, claims.getSubject(), claims.getExpiration().toInstant());
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

    public record JwtToken(String value, String jti, Instant expiresAt) {
    }

    public record ParsedToken(String jti, String subject, Instant expiresAt) {
    }
}
