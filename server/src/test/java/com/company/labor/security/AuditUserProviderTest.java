package com.company.labor.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.company.labor.module.system.domain.entity.SysUserDO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuditUserProviderTest {
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsNullWithoutLoginUserPrincipal() {
        assertNull(new AuditUserProvider().currentUserId());
    }

    @Test
    void returnsCurrentLoginUserId() {
        LoginUser loginUser = new LoginUser(user(), List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));

        assertEquals(100L, new AuditUserProvider().currentUserId());
    }

    private SysUserDO user() {
        SysUserDO user = new SysUserDO();
        user.setId(100L);
        user.setUsername("auditor");
        user.setStatus("active");
        return user;
    }
}
