package com.company.labor.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.service.SysUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {
    private static final String SECRET = "test-secret-with-more-than-thirty-two-bytes";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsInvalidBearerTokenBeforeHandlerChain() throws Exception {
        JwtAuthenticationFilter filter = filter(mock(SysUserService.class), new JwtTokenProvider(SECRET, 30), notRevoked());
        MockHttpServletRequest request = request("Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> invoked.set(true));

        assertFalse(invoked.get());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("登录凭证无效，请重新登录"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rejectsDisabledUserTokenBeforeHandlerChain() throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 30);
        String token = provider.createToken(loginUser(100L, "active"));
        SysUserService userService = mock(SysUserService.class);
        when(userService.loadLoginUserById(100L)).thenReturn(loginUser(100L, "inactive"));
        JwtAuthenticationFilter filter = filter(userService, provider, notRevoked());
        MockHttpServletRequest request = request("Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> invoked.set(true));

        assertFalse(invoked.get());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("账号已停用，请重新登录或联系管理员"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void authenticatesActiveUserAndContinuesHandlerChain() throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 30);
        String token = provider.createToken(loginUser(100L, "active"));
        SysUserService userService = mock(SysUserService.class);
        when(userService.loadLoginUserById(100L)).thenReturn(loginUser(100L, "active"));
        JwtAuthenticationFilter filter = filter(userService, provider, notRevoked());
        MockHttpServletRequest request = request("Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> invoked.set(true));

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
        assertEquals("tester", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void rejectsRevokedTokenBeforeHandlerChain() throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 30);
        String token = provider.createToken(loginUser(100L, "active"));
        TokenBlacklistService blacklist = mock(TokenBlacklistService.class);
        when(blacklist.isRevoked(provider.parse(token).jti())).thenReturn(true);
        JwtAuthenticationFilter filter = filter(mock(SysUserService.class), provider, blacklist);

        MockHttpServletRequest request = request("Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> invoked.set(true));

        assertFalse(invoked.get());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("登录已失效，请重新登录"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private JwtAuthenticationFilter filter(SysUserService userService, JwtTokenProvider provider, TokenBlacklistService blacklist) {
        return new JwtAuthenticationFilter(provider, userService, blacklist, new AuthenticationErrorWriter(new ObjectMapper()));
    }

    private TokenBlacklistService notRevoked() {
        TokenBlacklistService mock = mock(TokenBlacklistService.class);
        when(mock.isRevoked(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        return mock;
    }

    private MockHttpServletRequest request(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorization);
        return request;
    }

    private LoginUser loginUser(Long id, String status) {
        SysUserDO user = new SysUserDO();
        user.setId(id);
        user.setUsername("tester");
        user.setPassword("encoded");
        user.setStatus(status);
        return new LoginUser(user, List.of("ADMIN"));
    }
}
