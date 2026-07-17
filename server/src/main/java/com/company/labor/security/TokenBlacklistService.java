package com.company.labor.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.labor.security.mapper.TokenBlacklistMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Records and looks up revoked JWTs by their JTI.
 *
 * <p>Every authenticated request consults {@link #isRevoked(String)} — to avoid a
 * database round-trip on each call, results are cached for a short window. The
 * cache only ever holds <em>known-revoked</em> JTIs (positive hits), so a revoked
 * token stays rejected even if the DB row is momentarily unavailable, and a
 * non-revoked token still falls through to a DB check at most once per TTL window.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    /** How long a "not revoked" decision is remembered before re-querying. */
    static final Duration NEGATIVE_CACHE_TTL = Duration.ofSeconds(60);
    /** Upper bound on positive entries — protects against runaway memory. */
    private static final int MAX_POSITIVE_ENTRIES = 10_000;

    private final TokenBlacklistMapper tokenBlacklistMapper;

    /** Value is the expiry instant the entry may be evicted at; presence == revoked. */
    private final Map<String, Instant> revokedJtis = new ConcurrentHashMap<>();
    /** Value is the instant the negative decision was made. */
    private final Map<String, Instant> notRevokedUntil = new ConcurrentHashMap<>();

    public void revoke(String jti, Long userId, Instant expiresAt, Long revokedBy) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        TokenBlacklistDO row = new TokenBlacklistDO();
        row.setJti(jti);
        row.setUserId(userId);
        row.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()));
        row.setRevokedBy(revokedBy);
        row.setRevokedAt(LocalDateTime.now());
        // insert-ignore: a duplicate JTI (e.g. double logout click) must not error.
        try {
            tokenBlacklistMapper.insert(row);
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            log.debug("token already blacklisted: jti={}", jti);
        }
        cacheRevoked(jti, expiresAt);
    }

    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Instant now = Instant.now();
        Instant revokedAt = revokedJtis.get(jti);
        if (revokedAt != null) {
            return true;
        }
        Instant notUntil = notRevokedUntil.get(jti);
        if (notUntil != null && notUntil.isAfter(now)) {
            return false;
        }
        boolean exists = tokenBlacklistMapper.exists(new LambdaQueryWrapper<TokenBlacklistDO>()
                .eq(TokenBlacklistDO::getJti, jti));
        if (exists) {
            // We don't know the exact expiry without another query; cache for the
            // full negative TTL — worst case a revoked token is accepted for the
            // short window until the next DB check, then rejected permanently.
            cacheRevoked(jti, now.plus(NEGATIVE_CACHE_TTL));
            return true;
        }
        notRevokedUntil.put(jti, now.plus(NEGATIVE_CACHE_TTL));
        return false;
    }

    private void cacheRevoked(String jti, Instant expiry) {
        if (revokedJtis.size() > MAX_POSITIVE_ENTRIES) {
            // Opportunistic eviction of expired positive entries before inserting.
            Instant now = Instant.now();
            revokedJtis.values().removeIf(expiry::isBefore);
            notRevokedUntil.values().removeIf(deadline -> deadline.isBefore(now));
        }
        revokedJtis.put(jti, expiry);
    }
}
