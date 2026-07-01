package com.company.labor.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private LoginUser loginUser(String status) {
        SysUserDO user = new SysUserDO();
        user.setId(100L);
        user.setUsername("tester");
        user.setPassword("encoded");
        user.setStatus(status);
        return new LoginUser(user, List.of("ADMIN"));
    }
}
