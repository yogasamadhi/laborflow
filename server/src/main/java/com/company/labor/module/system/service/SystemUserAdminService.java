package com.company.labor.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.system.domain.dto.SysUserResetPasswordDTO;
import com.company.labor.module.system.domain.dto.SysUserRoleAssignDTO;
import com.company.labor.module.system.domain.dto.SysUserSaveDTO;
import com.company.labor.module.system.domain.entity.OperationLogDO;
import com.company.labor.module.system.domain.entity.SysRoleDO;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.domain.entity.SysUserRoleDO;
import com.company.labor.module.system.domain.vo.SysUserVO;
import com.company.labor.module.system.mapper.OperationLogMapper;
import com.company.labor.module.system.mapper.SysRoleMapper;
import com.company.labor.module.system.mapper.SysUserMapper;
import com.company.labor.module.system.mapper.SysUserRoleMapper;
import com.company.labor.security.LoginUser;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SystemUserAdminService {
    private static final String COL_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_DELETED = "deleted";
    private static final String COL_USER_ID = "user_id";
    private static final String COL_STATUS = "status";
    private static final String COL_PASSWORD = "password";
    private static final String COL_PASSWORD_UPDATED_AT = "password_updated_at";
    private static final String COL_FAILED_LOGIN_COUNT = "failed_login_count";
    private static final String COL_LOCKED_UNTIL = "locked_until";
    private static final String COL_UPDATED_AT = "updated_at";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final OperationLogMapper operationLogMapper;
    private final PasswordEncoder passwordEncoder;

    public Page<SysUserVO> page(long page, long pageSize, String keyword, String status) {
        QueryWrapper<SysUserDO> query = new QueryWrapper<SysUserDO>()
                .eq(COL_DELETED, 0)
                .eq(StringUtils.hasText(status), COL_STATUS, status)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(COL_USERNAME, keyword)
                        .or()
                        .like("nickname", keyword))
                .orderByDesc("created_at");
        Page<SysUserDO> users = userMapper.selectPage(new Page<>(page, pageSize), query);
        List<Long> userIds = users.getRecords().stream().map(SysUserDO::getId).toList();
        Map<Long, List<SysRoleDO>> roles = rolesByUserId(userIds);
        Page<SysUserVO> result = new Page<>(users.getCurrent(), users.getSize(), users.getTotal());
        result.setRecords(users.getRecords().stream().map(user -> toVO(user, roles.getOrDefault(user.getId(), List.of()))).toList());
        return result;
    }

    public SysUserVO create(SysUserSaveDTO dto, LoginUser operator) {
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException("初始密码必填");
        }
        ensureUsernameAvailable(dto.getUsername(), null);
        SysUserDO user = new SysUserDO();
        fillUser(user, dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setFailedLoginCount(0);
        user.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "active");
        user.setDeleted(0);
        userMapper.insert(user);
        assignRoles(user.getId(), dto.getRoleIds(), operator);
        log("create_user", user.getId(), operator, "创建用户: " + user.getUsername());
        return toVO(user, rolesForUser(user.getId()));
    }

    public SysUserVO update(Long id, SysUserSaveDTO dto, LoginUser operator) {
        SysUserDO user = requireUser(id);
        ensureUsernameAvailable(dto.getUsername(), id);
        fillUser(user, dto);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        assignRoles(id, dto.getRoleIds(), operator);
        log("update_user", id, operator, "更新用户: " + user.getUsername());
        return toVO(user, rolesForUser(id));
    }

    public void assignRoles(Long id, SysUserRoleAssignDTO dto, LoginUser operator) {
        requireUser(id);
        assignRoles(id, dto.getRoleIds(), operator);
        log("assign_roles", id, operator, "分配用户角色");
    }

    public void resetPassword(Long id, SysUserResetPasswordDTO dto, LoginUser operator) {
        SysUserDO user = requireUser(id);
        LocalDateTime now = LocalDateTime.now();
        userMapper.update(null, new UpdateWrapper<SysUserDO>()
                .eq(COL_ID, id)
                .set(COL_PASSWORD, passwordEncoder.encode(dto.getNewPassword()))
                .set(COL_PASSWORD_UPDATED_AT, now)
                .set(COL_FAILED_LOGIN_COUNT, 0)
                .set(COL_LOCKED_UNTIL, null)
                .set(COL_UPDATED_AT, now));
        log("reset_password", id, operator, "重置用户密码: " + user.getUsername());
    }

    public void disable(Long id, LoginUser operator) {
        if (operator != null && operator.getUser().getId().equals(id)) {
            throw new BusinessException("不能停用当前登录用户");
        }
        SysUserDO user = requireUser(id);
        userMapper.update(null, new UpdateWrapper<SysUserDO>()
                .eq(COL_ID, id)
                .set(COL_STATUS, "inactive")
                .set(COL_UPDATED_AT, LocalDateTime.now()));
        log("disable_user", id, operator, "停用用户: " + user.getUsername());
    }

    private void fillUser(SysUserDO user, SysUserSaveDTO dto) {
        user.setUsername(dto.getUsername());
        user.setEmployeeId(dto.getEmployeeId());
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        if (StringUtils.hasText(dto.getStatus())) {
            user.setStatus(dto.getStatus());
        }
    }

    private void assignRoles(Long userId, List<Long> roleIds, LoginUser operator) {
        if (CollectionUtils.isEmpty(roleIds)) {
            throw new BusinessException("至少选择一个角色");
        }
        List<SysRoleDO> roles = roleMapper.selectBatchIds(roleIds);
        if (roles.size() != roleIds.stream().distinct().count()) {
            throw new BusinessException("角色不存在");
        }
        boolean hasInactiveRole = roles.stream().anyMatch(role -> !"active".equals(role.getStatus()));
        if (hasInactiveRole) {
            throw new BusinessException("不能分配已停用角色");
        }
        userRoleMapper.delete(new QueryWrapper<SysUserRoleDO>().eq(COL_USER_ID, userId));
        for (Long roleId : roleIds.stream().distinct().toList()) {
            SysUserRoleDO relation = new SysUserRoleDO();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }

    private SysUserDO requireUser(Long id) {
        SysUserDO user = userMapper.selectById(id);
        if (user == null || deleted(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private void ensureUsernameAvailable(String username, Long currentId) {
        SysUserDO existing = userMapper.selectOne(new QueryWrapper<SysUserDO>()
                .eq(COL_USERNAME, username)
                .eq(COL_DELETED, 0));
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new BusinessException("账号已存在");
        }
    }

    private Map<Long, List<SysRoleDO>> rolesByUserId(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUserRoleDO> relations = userRoleMapper.selectList(new QueryWrapper<SysUserRoleDO>().in(COL_USER_ID, userIds));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, SysRoleDO> roles = roleMapper.selectBatchIds(relations.stream().map(SysUserRoleDO::getRoleId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(SysRoleDO::getId, Function.identity()));
        return relations.stream()
                .filter(relation -> roles.containsKey(relation.getRoleId()))
                .collect(Collectors.groupingBy(
                        SysUserRoleDO::getUserId,
                        Collectors.mapping(relation -> roles.get(relation.getRoleId()), Collectors.toList())));
    }

    private List<SysRoleDO> rolesForUser(Long userId) {
        return rolesByUserId(List.of(userId)).getOrDefault(userId, List.of());
    }

    private SysUserVO toVO(SysUserDO user, List<SysRoleDO> roles) {
        return new SysUserVO(
                user.getId(),
                user.getUsername(),
                user.getEmployeeId(),
                user.getNickname(),
                user.getPhone(),
                user.getStatus(),
                roles.stream().map(SysRoleDO::getId).toList(),
                roles.stream().map(SysRoleDO::getCode).toList(),
                user.getLastLoginAt(),
                user.getLockedUntil(),
                user.getPasswordUpdatedAt(),
                user.getCreatedAt());
    }

    private boolean deleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }

    private void log(String action, Long userId, LoginUser operator, String content) {
        OperationLogDO log = new OperationLogDO();
        log.setModule("system_user");
        log.setBusinessId(userId);
        log.setAction(action);
        if (operator != null) {
            log.setOperatorId(operator.getUser().getId());
            log.setOperatorName(operator.getUsername());
        }
        log.setContent(content);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
