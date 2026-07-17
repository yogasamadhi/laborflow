package com.company.labor.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.security.LoginUser;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class LoginSecurityService {
    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final String COL_ID = "id";
    private static final String COL_FAILED_LOGIN_COUNT = "failed_login_count";
    private static final String COL_LOCKED_UNTIL = "locked_until";
    private static final String COL_LAST_LOGIN_AT = "last_login_at";
    private static final String COL_LAST_LOGIN_IP = "last_login_ip";
    private static final String COL_UPDATED_AT = "updated_at";

    private final SysUserService userService;
    private final Clock clock;

    @Autowired
    public LoginSecurityService(SysUserService userService) {
        this(userService, Clock.systemDefaultZone());
    }

    LoginSecurityService(SysUserService userService, Clock clock) {
        this.userService = userService;
        this.clock = clock;
    }

    public void checkLoginAllowed(String username) {
        SysUserDO user = findUser(username);
        if (user == null) {
            return;
        }
        if (!"active".equals(user.getStatus())) {
            throw new DisabledException("账号已停用");
        }
        LocalDateTime now = now();
        LocalDateTime lockedUntil = user.getLockedUntil();
        if (lockedUntil == null) {
            return;
        }
        if (lockedUntil.isAfter(now)) {
            throw new LockedException("账号已临时锁定，请稍后再试");
        }
        clearFailedState(user.getId());
    }

    public boolean recordLoginFailure(String username) {
        SysUserDO user = findUser(username);
        if (user == null || !"active".equals(user.getStatus())) {
            return false;
        }
        LocalDateTime now = now();
        int failedCount = defaultCount(user.getFailedLoginCount()) + 1;
        LocalDateTime lockedUntil = failedCount >= MAX_FAILED_ATTEMPTS ? now.plus(LOCK_DURATION) : null;
        UpdateWrapper<SysUserDO> update = updateById(user.getId())
                .set(COL_FAILED_LOGIN_COUNT, failedCount)
                .set(COL_UPDATED_AT, now);
        if (lockedUntil != null) {
            update.set(COL_LOCKED_UNTIL, lockedUntil);
            log.warn("account locked after {} failed attempts: username='{}' lockedUntil={}", failedCount, username, lockedUntil);
        }
        userService.update(update);
        return lockedUntil != null;
    }

    public void recordLoginSuccess(LoginUser loginUser, String clientIp) {
        LocalDateTime now = now();
        userService.update(updateById(loginUser.getUser().getId())
                .set(COL_FAILED_LOGIN_COUNT, 0)
                .set(COL_LOCKED_UNTIL, null)
                .set(COL_LAST_LOGIN_AT, now)
                .set(COL_LAST_LOGIN_IP, clientIp)
                .set(COL_UPDATED_AT, now));
    }

    private void clearFailedState(Long userId) {
        userService.update(updateById(userId)
                .set(COL_FAILED_LOGIN_COUNT, 0)
                .set(COL_LOCKED_UNTIL, null)
                .set(COL_UPDATED_AT, now()));
    }

    private UpdateWrapper<SysUserDO> updateById(Long userId) {
        return new UpdateWrapper<SysUserDO>().eq(COL_ID, userId);
    }

    private SysUserDO findUser(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userService.getOne(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getUsername, username)
                .eq(SysUserDO::getDeleted, 0), false);
    }

    private int defaultCount(Integer count) {
        return count == null ? 0 : count;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
