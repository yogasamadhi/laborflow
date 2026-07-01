package com.company.labor.module.system.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.company.labor.module.system.domain.dto.SysUserResetPasswordDTO;
import com.company.labor.module.system.domain.dto.SysUserSaveDTO;
import com.company.labor.module.system.domain.entity.SysRoleDO;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.domain.entity.SysUserRoleDO;
import com.company.labor.module.system.mapper.SysRoleMapper;
import com.company.labor.module.system.mapper.SysUserMapper;
import com.company.labor.module.system.mapper.SysUserRoleMapper;
import com.company.labor.security.LoginUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class SystemUserAdminServiceTest {
    @Test
    void createsUserWithEncodedPasswordAndRoles() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        OperationLogService logService = mock(OperationLogService.class);
        when(userMapper.insert(org.mockito.ArgumentMatchers.<SysUserDO>any())).thenAnswer(invocation -> {
            SysUserDO user = invocation.getArgument(0);
            user.setId(2L);
            return 1;
        });
        when(roleMapper.selectBatchIds(List.of(1001L))).thenReturn(List.of(role(1001L)));
        when(userRoleMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());
        SystemUserAdminService service = service(userMapper, roleMapper, userRoleMapper, logService);

        service.create(saveDto(), loginUser(1L));

        verify(userMapper).insert(any(SysUserDO.class));
        verify(userRoleMapper).insert(org.mockito.ArgumentMatchers.<SysUserRoleDO>any());
        verify(logService).record(any(), any(), any(), any(LoginUser.class), any());
    }

    @Test
    void resetsPasswordAndClearsLockState() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        when(userMapper.selectById(2L)).thenReturn(user(2L));
        SystemUserAdminService service = service(userMapper, mock(SysRoleMapper.class), mock(SysUserRoleMapper.class), mock(OperationLogService.class));
        SysUserResetPasswordDTO dto = new SysUserResetPasswordDTO();
        dto.setNewPassword("new-password");

        service.resetPassword(2L, dto, loginUser(1L));

        verify(userMapper).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    void refusesToDisableCurrentUser() {
        SystemUserAdminService service = service(mock(SysUserMapper.class), mock(SysRoleMapper.class), mock(SysUserRoleMapper.class), mock(OperationLogService.class));

        assertThrows(RuntimeException.class, () -> service.disable(1L, loginUser(1L)));
    }

    private SystemUserAdminService service(SysUserMapper userMapper, SysRoleMapper roleMapper, SysUserRoleMapper userRoleMapper, OperationLogService logService) {
        return new SystemUserAdminService(userMapper, roleMapper, userRoleMapper, logService, new BCryptPasswordEncoder());
    }

    private SysUserSaveDTO saveDto() {
        SysUserSaveDTO dto = new SysUserSaveDTO();
        dto.setUsername("new_user");
        dto.setPassword("initial-password");
        dto.setNickname("新用户");
        dto.setRoleIds(List.of(1001L));
        return dto;
    }

    private SysRoleDO role(Long id) {
        SysRoleDO role = new SysRoleDO();
        role.setId(id);
        role.setCode("ADMIN");
        role.setStatus("active");
        return role;
    }

    private SysUserDO user(Long id) {
        SysUserDO user = new SysUserDO();
        user.setId(id);
        user.setUsername("user" + id);
        user.setStatus("active");
        user.setDeleted(0);
        return user;
    }

    private LoginUser loginUser(Long id) {
        return new LoginUser(user(id), List.of("ADMIN"));
    }
}
