package com.company.labor.module.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthRegistrationServiceTest {
    @Test
    void registersActiveEmployeeAccountWithEncodedPassword() {
        SysUserMapper userMapper = org.mockito.Mockito.mock(SysUserMapper.class);
        SysRoleMapper roleMapper = org.mockito.Mockito.mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = org.mockito.Mockito.mock(SysUserRoleMapper.class);
        OperationLogService operationLogService = org.mockito.Mockito.mock(OperationLogService.class);
        PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(roleMapper.selectOne(any(QueryWrapper.class))).thenReturn(employeeRole());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userMapper.insert(any(SysUserDO.class))).thenAnswer(invocation -> {
            SysUserDO user = invocation.getArgument(0);
            user.setId(77L);
            return 1;
        });
        AuthRegistrationService service = new AuthRegistrationService(userMapper, roleMapper, userRoleMapper, operationLogService, passwordEncoder);

        LoginUser loginUser = service.register(registerDto());

        assertThat(loginUser.getUsername()).isEqualTo("new_user");
        assertThat(loginUser.getRoles()).containsExactly("EMPLOYEE");
        ArgumentCaptor<SysUserDO> userCaptor = ArgumentCaptor.forClass(SysUserDO.class);
        verify(userMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(userCaptor.getValue().getStatus()).isEqualTo("active");
        assertThat(userCaptor.getValue().getFailedLoginCount()).isZero();
        ArgumentCaptor<SysUserRoleDO> relationCaptor = ArgumentCaptor.forClass(SysUserRoleDO.class);
        verify(userRoleMapper).insert(relationCaptor.capture());
        assertThat(relationCaptor.getValue().getUserId()).isEqualTo(77L);
        assertThat(relationCaptor.getValue().getRoleId()).isEqualTo(1005L);
        verify(operationLogService).record(eq("auth"), eq(77L), eq("register_user"), isNull(LoginUser.class), contains("new_user"));
    }

    @Test
    void refusesDuplicateUsername() {
        SysUserMapper userMapper = org.mockito.Mockito.mock(SysUserMapper.class);
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(new SysUserDO());
        SysUserRoleMapper userRoleMapper = org.mockito.Mockito.mock(SysUserRoleMapper.class);
        AuthRegistrationService service = new AuthRegistrationService(userMapper, org.mockito.Mockito.mock(SysRoleMapper.class), userRoleMapper,
                org.mockito.Mockito.mock(OperationLogService.class), org.mockito.Mockito.mock(PasswordEncoder.class));

        assertThrows(BusinessException.class, () -> service.register(registerDto()));

        verify(userRoleMapper, never()).insert(org.mockito.ArgumentMatchers.<SysUserRoleDO>any());
    }

    @Test
    void refusesMismatchedPasswordConfirmation() {
        RegisterDTO dto = registerDto();
        dto.setConfirmPassword("another-password");
        SysUserMapper userMapper = org.mockito.Mockito.mock(SysUserMapper.class);
        AuthRegistrationService service = new AuthRegistrationService(userMapper, org.mockito.Mockito.mock(SysRoleMapper.class), org.mockito.Mockito.mock(SysUserRoleMapper.class),
                org.mockito.Mockito.mock(OperationLogService.class), org.mockito.Mockito.mock(PasswordEncoder.class));

        assertThrows(BusinessException.class, () -> service.register(dto));

        verify(userMapper, never()).insert(org.mockito.ArgumentMatchers.<SysUserDO>any());
    }

    private RegisterDTO registerDto() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername(" new_user ");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setNickname("新用户");
        dto.setPhone("13800138000");
        return dto;
    }

    private SysRoleDO employeeRole() {
        SysRoleDO role = new SysRoleDO();
        role.setId(1005L);
        role.setCode("EMPLOYEE");
        role.setStatus("active");
        role.setDeleted(0);
        return role;
    }
}
