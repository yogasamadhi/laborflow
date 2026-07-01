package com.company.labor.module.system.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.security.LoginUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

class LoginSecurityServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-01T03:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Test
    void rejectsActiveUserWhileTemporaryLockIsValid() {
        SysUserService userService = userService(user("active", 5, now().plusMinutes(10)));
        LoginSecurityService service = new LoginSecurityService(userService, CLOCK);

        assertThrows(LockedException.class, () -> service.checkLoginAllowed("admin"));
    }

    @Test
    void clearsExpiredLockBeforeLogin() {
        SysUserService userService = userService(user("active", 5, now().minusMinutes(1)));
        LoginSecurityService service = new LoginSecurityService(userService, CLOCK);

        assertDoesNotThrow(() -> service.checkLoginAllowed("admin"));

        verify(userService).update(any(UpdateWrapper.class));
    }

    @Test
    void rejectsInactiveUserBeforePasswordCheck() {
        SysUserService userService = userService(user("inactive", 0, null));
        LoginSecurityService service = new LoginSecurityService(userService, CLOCK);

        assertThrows(DisabledException.class, () -> service.checkLoginAllowed("admin"));
    }

    @Test
    void locksAccountAfterMaxFailures() {
        SysUserService userService = userService(user("active", 4, null));
        LoginSecurityService service = new LoginSecurityService(userService, CLOCK);

        assertTrue(service.recordLoginFailure("admin"));

        verify(userService).update(any(UpdateWrapper.class));
    }

    @Test
    void keepsAccountUnlockedBeforeMaxFailures() {
        SysUserService userService = userService(user("active", 1, null));
        LoginSecurityService service = new LoginSecurityService(userService, CLOCK);

        assertFalse(service.recordLoginFailure("admin"));

        verify(userService).update(any(UpdateWrapper.class));
    }

    @Test
    void recordsSuccessfulLogin() {
        SysUserService userService = mock(SysUserService.class);
        LoginSecurityService service = new LoginSecurityService(userService, CLOCK);
        LoginUser loginUser = new LoginUser(user("active", 3, now().plusMinutes(5)), List.of("ADMIN"));

        service.recordLoginSuccess(loginUser, "10.0.0.1");

        verify(userService).update(any(UpdateWrapper.class));
    }

    private SysUserService userService(SysUserDO user) {
        SysUserService userService = mock(SysUserService.class);
        when(userService.getOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(user);
        return userService;
    }

    private SysUserDO user(String status, Integer failedCount, LocalDateTime lockedUntil) {
        SysUserDO user = new SysUserDO();
        user.setId(100L);
        user.setUsername("admin");
        user.setPassword("encoded");
        user.setStatus(status);
        user.setFailedLoginCount(failedCount);
        user.setLockedUntil(lockedUntil);
        return user;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(CLOCK);
    }
}
