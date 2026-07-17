package com.company.labor.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.labor.module.system.domain.entity.SysRoleDO;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.domain.entity.SysUserRoleDO;
import com.company.labor.module.system.mapper.SysRoleMapper;
import com.company.labor.module.system.mapper.SysUserMapper;
import com.company.labor.module.system.mapper.SysUserRoleMapper;
import com.company.labor.module.system.service.OperationLogService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapAdminRunner implements ApplicationRunner {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ACTIVE = "active";
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 64;
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_LENGTH = 64;

    private final BootstrapAdminProperties properties;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean hasUsername = StringUtils.hasText(properties.getUsername());
        boolean hasPassword = StringUtils.hasText(properties.getPassword());
        if (hasUsername != hasPassword) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD must be configured together");
        }
        if (userMapper.hasActiveAdministrator()) {
            return;
        }
        if (!hasUsername) {
            throw new IllegalStateException("No active ADMIN user exists; configure BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD for one startup");
        }

        String username = properties.getUsername().trim();
        log.info("bootstrapping first administrator: username='{}'", username);
        validateCredentials(username, properties.getPassword());
        SysRoleDO adminRole = requireAdminRole();
        SysUserDO user = userMapper.selectOne(new QueryWrapper<SysUserDO>().eq("username", username));
        if (user == null) {
            user = createAdmin(username, properties.getPassword());
        } else {
            reactivateExistingAdmin(user, properties.getPassword());
        }

        userRoleMapper.delete(new QueryWrapper<SysUserRoleDO>().eq("user_id", user.getId()));
        SysUserRoleDO relation = new SysUserRoleDO();
        relation.setUserId(user.getId());
        relation.setRoleId(adminRole.getId());
        userRoleMapper.insert(relation);
        operationLogService.record("system_user", user.getId(), "bootstrap_admin", null,
                "启动期创建或恢复首个管理员: " + username);
    }

    private void validateCredentials(String username, String password) {
        if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_USERNAME must be 3-64 characters");
        }
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be 12-64 characters");
        }
    }

    private SysRoleDO requireAdminRole() {
        SysRoleDO role = roleMapper.selectOne(new QueryWrapper<SysRoleDO>()
                .eq("code", ADMIN_ROLE)
                .eq("status", ACTIVE));
        if (role == null) {
            throw new IllegalStateException("Active ADMIN role is required before bootstrap");
        }
        return role;
    }

    private SysUserDO createAdmin(String username, String password) {
        LocalDateTime now = LocalDateTime.now();
        SysUserDO user = new SysUserDO();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname("系统管理员");
        user.setStatus(ACTIVE);
        user.setFailedLoginCount(0);
        user.setPasswordUpdatedAt(now);
        user.setDeleted(0);
        userMapper.insert(user);
        return user;
    }

    private void reactivateExistingAdmin(SysUserDO user, String password) {
        if (!roleMapper.selectRoleCodesByUserId(user.getId()).contains(ADMIN_ROLE)) {
            throw new IllegalStateException("Bootstrap username belongs to an account without ADMIN role");
        }
        LocalDateTime now = LocalDateTime.now();
        user.setPassword(passwordEncoder.encode(password));
        user.setStatus(ACTIVE);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setPasswordUpdatedAt(now);
        user.setUpdatedAt(now);
        userMapper.updateById(user);
    }
}
