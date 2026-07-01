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
        JwtAuthenticationFilter filter = filter(mock(SysUserService.class), new JwtTokenProvider(SECRET, 30));
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
        JwtAuthenticationFilter filter = filter(userService, provider);
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
        JwtAuthenticationFilter filter = filter(userService, provider);
        MockHttpServletRequest request = request("Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> invoked.set(true));

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
        assertEquals("tester", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private JwtAuthenticationFilter filter(SysUserService userService, JwtTokenProvider provider) {
        return new JwtAuthenticationFilter(provider, userService, new AuthenticationErrorWriter(new ObjectMapper()));
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
