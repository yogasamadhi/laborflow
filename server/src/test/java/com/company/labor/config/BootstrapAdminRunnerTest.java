package com.company.labor.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.labor.module.system.domain.entity.SysRoleDO;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.domain.entity.SysUserRoleDO;
import com.company.labor.module.system.mapper.SysRoleMapper;
import com.company.labor.module.system.mapper.SysUserMapper;
import com.company.labor.module.system.mapper.SysUserRoleMapper;
import com.company.labor.module.system.service.OperationLogService;
import com.company.labor.security.LoginUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class BootstrapAdminRunnerTest {
    @Test
    void createsAdministratorWhenNoActiveAdministratorExists() {
        BootstrapAdminProperties properties = properties("bootstrap_admin", "long-bootstrap-password");
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        OperationLogService logService = mock(OperationLogService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(userMapper.hasActiveAdministrator()).thenReturn(false);
        when(roleMapper.selectOne(any(QueryWrapper.class))).thenReturn(adminRole());
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(passwordEncoder.encode("long-bootstrap-password")).thenReturn("encoded-password");
        when(userMapper.insert(any(SysUserDO.class))).thenAnswer(invocation -> {
            SysUserDO user = invocation.getArgument(0);
            user.setId(88L);
            return 1;
        });

        runner(properties, userMapper, roleMapper, userRoleMapper, logService, passwordEncoder).run(null);

        ArgumentCaptor<SysUserDO> userCaptor = ArgumentCaptor.forClass(SysUserDO.class);
        ArgumentCaptor<SysUserRoleDO> relationCaptor = ArgumentCaptor.forClass(SysUserRoleDO.class);
        verify(userMapper).insert(userCaptor.capture());
        verify(userRoleMapper).insert(relationCaptor.capture());
        verify(logService).record(eq("system_user"), eq(88L), eq("bootstrap_admin"), isNull(LoginUser.class), contains("bootstrap_admin"));
        assertThat(userCaptor.getValue().getStatus()).isEqualTo("active");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(relationCaptor.getValue().getUserId()).isEqualTo(88L);
        assertThat(relationCaptor.getValue().getRoleId()).isEqualTo(1001L);
    }

    @Test
    void refusesToStartWithoutAdministratorOrCompleteBootstrapCredentials() {
        BootstrapAdminProperties properties = properties("bootstrap_admin", null);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        when(userMapper.hasActiveAdministrator()).thenReturn(false);

        assertThatThrownBy(() -> runner(properties, userMapper, mock(SysRoleMapper.class), mock(SysUserRoleMapper.class),
                mock(OperationLogService.class), mock(PasswordEncoder.class)).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("configured together");
    }

    @Test
    void skipsBootstrapWhenAnAdministratorAlreadyExists() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        when(userMapper.hasActiveAdministrator()).thenReturn(true);

        runner(properties(null, null), userMapper, mock(SysRoleMapper.class), mock(SysUserRoleMapper.class),
                mock(OperationLogService.class), mock(PasswordEncoder.class)).run(null);

        verify(userMapper, never()).insert(any(SysUserDO.class));
    }

    @Test
    void onlyReactivatesAnExistingAdministratorAccount() {
        BootstrapAdminProperties properties = properties("recovery_admin", "long-bootstrap-password");
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        SysUserDO user = new SysUserDO();
        user.setId(66L);
        user.setUsername("recovery_admin");
        user.setStatus("inactive");
        when(userMapper.hasActiveAdministrator()).thenReturn(false);
        when(roleMapper.selectOne(any(QueryWrapper.class))).thenReturn(adminRole());
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
        when(roleMapper.selectRoleCodesByUserId(66L)).thenReturn(List.of("ADMIN"));
        when(passwordEncoder.encode("long-bootstrap-password")).thenReturn("encoded-password");

        runner(properties, userMapper, roleMapper, userRoleMapper, mock(OperationLogService.class), passwordEncoder).run(null);

        verify(userMapper).updateById(user);
        assertThat(user.getStatus()).isEqualTo("active");
        assertThat(user.getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void refusesToElevateAnExistingNonAdministratorAccount() {
        BootstrapAdminProperties properties = properties("employee_account", "long-bootstrap-password");
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserDO user = new SysUserDO();
        user.setId(67L);
        user.setUsername("employee_account");
        when(userMapper.hasActiveAdministrator()).thenReturn(false);
        when(roleMapper.selectOne(any(QueryWrapper.class))).thenReturn(adminRole());
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
        when(roleMapper.selectRoleCodesByUserId(67L)).thenReturn(List.of("EMPLOYEE"));

        assertThatThrownBy(() -> runner(properties, userMapper, roleMapper, mock(SysUserRoleMapper.class),
                mock(OperationLogService.class), mock(PasswordEncoder.class)).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("without ADMIN role");

        verify(userMapper, never()).updateById(any(SysUserDO.class));
    }

    private BootstrapAdminRunner runner(BootstrapAdminProperties properties, SysUserMapper userMapper,
                                        SysRoleMapper roleMapper, SysUserRoleMapper userRoleMapper,
                                        OperationLogService logService, PasswordEncoder passwordEncoder) {
        return new BootstrapAdminRunner(properties, userMapper, roleMapper, userRoleMapper, logService, passwordEncoder);
    }

    private BootstrapAdminProperties properties(String username, String password) {
        BootstrapAdminProperties properties = new BootstrapAdminProperties();
        properties.setUsername(username);
        properties.setPassword(password);
        return properties;
    }

    private SysRoleDO adminRole() {
        SysRoleDO role = new SysRoleDO();
        role.setId(1001L);
        role.setCode("ADMIN");
        role.setStatus("active");
        return role;
    }
}
