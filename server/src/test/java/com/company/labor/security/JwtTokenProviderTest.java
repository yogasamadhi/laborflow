package com.company.labor.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.labor.module.system.domain.entity.SysUserDO;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {
    private static final String SECRET = "test-secret-with-more-than-thirty-two-bytes";

    @Test
    void createsTokenWithExpiryAndParsesUserId() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 30);
        JwtTokenProvider.JwtToken token = provider.createLoginToken(loginUser("active"));

        assertNotNull(token.value());
        assertNotNull(token.expiresAt());
        assertEquals(100L, provider.parseUserId(token.value()));
    }

    @Test
    void rejectsExpiredTokenWithClearMessage() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, -1);
        String token = provider.createToken(loginUser("active"));

        JwtAuthenticationException ex = assertThrows(JwtAuthenticationException.class, () -> provider.parseUserId(token));

        assertEquals("登录已过期，请重新登录", ex.getMessage());
    }

    @Test
    void rejectsMalformedTokenWithClearMessage() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 30);

        JwtAuthenticationException ex = assertThrows(JwtAuthenticationException.class, () -> provider.parseUserId("bad-token"));

        assertEquals("登录凭证无效，请重新登录", ex.getMessage());
    }

    @Test
    void constructorRejectsBlankSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("", 30));
    }

    @Test
    void constructorRejectsShortSecretInsteadOfPadding() {
        // A 10-byte secret must be rejected outright — it must NOT be padded to 32
        // bytes with zeros (the old behaviour silently weakened short secrets).
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("short12345", 30));
    }

    @Test
    void tokenCarriesUniqueJti() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 30);
        JwtTokenProvider.JwtToken a = provider.createLoginToken(loginUser("active"));
        JwtTokenProvider.JwtToken b = provider.createLoginToken(loginUser("active"));

        assertNotNull(a.jti());
        assertNotEquals(a.jti(), b.jti());

        JwtTokenProvider.ParsedToken parsed = provider.parse(a.value());
        assertEquals(a.jti(), parsed.jti());
        assertEquals("100", parsed.subject());
    }

    private LoginUser loginUser(String status) {
        SysUserDO user = new SysUserDO();
        user.setId(100L);
        user.setUsername("tester");
        user.setPassword("encoded");
        user.setStatus(status);
        return new LoginUser(user, List.of("ADMIN"));
    }
}
