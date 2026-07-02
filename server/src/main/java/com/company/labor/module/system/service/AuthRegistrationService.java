package com.company.labor.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.system.domain.dto.RegisterDTO;
import com.company.labor.module.system.domain.entity.SysRoleDO;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.domain.entity.SysUserRoleDO;
import com.company.labor.module.system.mapper.SysRoleMapper;
import com.company.labor.module.system.mapper.SysUserMapper;
import com.company.labor.module.system.mapper.SysUserRoleMapper;
import com.company.labor.security.LoginUser;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthRegistrationService {
    private static final String DEFAULT_REGISTER_ROLE = "EMPLOYEE";
    private static final String COL_USERNAME = "username";
    private static final String COL_CODE = "code";
    private static final String COL_STATUS = "status";
    private static final String COL_DELETED = "deleted";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;

    public LoginUser register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        String username = normalize(dto.getUsername());
        ensureUsernameAvailable(username);
        SysRoleDO role = requireDefaultRole();

        SysUserDO user = new SysUserDO();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(normalizeNullable(dto.getNickname()));
        user.setPhone(normalizeNullable(dto.getPhone()));
        user.setStatus("active");
        user.setFailedLoginCount(0);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        userMapper.insert(user);

        SysUserRoleDO relation = new SysUserRoleDO();
        relation.setUserId(user.getId());
        relation.setRoleId(role.getId());
        userRoleMapper.insert(relation);
        operationLogService.record("auth", user.getId(), "register_user", null, "用户自助注册: " + username);
        return new LoginUser(user, List.of(role.getCode()));
    }

    private void ensureUsernameAvailable(String username) {
        SysUserDO existing = userMapper.selectOne(new QueryWrapper<SysUserDO>()
                .eq(COL_USERNAME, username)
                .eq(COL_DELETED, 0));
        if (existing != null) {
            throw new BusinessException("账号已存在");
        }
    }

    private SysRoleDO requireDefaultRole() {
        SysRoleDO role = roleMapper.selectOne(new QueryWrapper<SysRoleDO>()
                .eq(COL_CODE, DEFAULT_REGISTER_ROLE)
                .eq(COL_STATUS, "active")
                .eq(COL_DELETED, 0));
        if (role == null) {
            throw new BusinessException("默认注册角色未配置");
        }
        return role;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
