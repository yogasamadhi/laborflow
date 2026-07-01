package com.company.labor.module.system.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.company.labor.module.employee.domain.entity.EmployeeDO;
import com.company.labor.module.employee.mapper.EmployeeMapper;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.domain.entity.WechatUserBindingDO;
import com.company.labor.module.system.mapper.SysUserMapper;
import com.company.labor.module.system.mapper.WechatUserBindingMapper;
import com.company.labor.security.LoginUser;
import java.util.List;
import org.junit.jupiter.api.Test;

class WechatBindingServiceTest {
    @Test
    void bindsPendingOpenidAfterMiniappLogin() {
        WechatUserBindingMapper bindingMapper = mock(WechatUserBindingMapper.class);
        OperationLogService logService = mock(OperationLogService.class);
        when(bindingMapper.selectOne(any(QueryWrapper.class))).thenReturn(binding("pending"));
        WechatBindingService service = service(bindingMapper, logService);

        service.bindLoginUser("openid", "微信昵称", loginUser());

        verify(bindingMapper).update(isNull(), any(UpdateWrapper.class));
        verify(logService).record(any(), any(), any(), any(LoginUser.class), any());
    }

    @Test
    void rejectsDisabledOpenidSelfBinding() {
        WechatUserBindingMapper bindingMapper = mock(WechatUserBindingMapper.class);
        when(bindingMapper.selectOne(any(QueryWrapper.class))).thenReturn(binding("inactive"));
        WechatBindingService service = service(bindingMapper, mock(OperationLogService.class));

        assertThrows(RuntimeException.class, () -> service.bindLoginUser("openid", "微信昵称", loginUser()));
    }

    @Test
    void unbindsAndAuditsBinding() {
        WechatUserBindingMapper bindingMapper = mock(WechatUserBindingMapper.class);
        OperationLogService logService = mock(OperationLogService.class);
        when(bindingMapper.selectById(1L)).thenReturn(binding("active"));
        WechatBindingService service = service(bindingMapper, logService);

        service.unbind(1L, loginUser());

        verify(bindingMapper).update(isNull(), any(UpdateWrapper.class));
        verify(logService).record(any(), any(), any(), any(LoginUser.class), any());
    }

    private WechatBindingService service(WechatUserBindingMapper bindingMapper, OperationLogService logService) {
        EmployeeMapper employeeMapper = mock(EmployeeMapper.class);
        EmployeeDO employee = new EmployeeDO();
        employee.setId(20L);
        employee.setDeleted(0);
        when(employeeMapper.selectById(20L)).thenReturn(employee);
        return new WechatBindingService(bindingMapper, mock(SysUserMapper.class), employeeMapper, logService);
    }

    private WechatUserBindingDO binding(String status) {
        WechatUserBindingDO binding = new WechatUserBindingDO();
        binding.setId(1L);
        binding.setOpenid("openid");
        binding.setStatus(status);
        binding.setDeleted(0);
        return binding;
    }

    private LoginUser loginUser() {
        SysUserDO user = new SysUserDO();
        user.setId(10L);
        user.setUsername("admin");
        user.setNickname("管理员");
        user.setEmployeeId(20L);
        user.setStatus("active");
        return new LoginUser(user, List.of("ADMIN"));
    }
}
