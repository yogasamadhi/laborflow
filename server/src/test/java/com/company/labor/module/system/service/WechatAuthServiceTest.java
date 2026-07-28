package com.company.labor.module.system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.labor.module.system.domain.entity.WechatUserBindingDO;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.domain.vo.WechatSessionVO;
import com.company.labor.module.system.mapper.SysUserMapper;
import com.company.labor.module.system.mapper.WechatUserBindingMapper;
import org.junit.jupiter.api.Test;

class WechatAuthServiceTest {
    @Test
    void createsPendingBindingForNewOpenid() {
        WechatMiniappClient client = mock(WechatMiniappClient.class);
        WechatUserBindingMapper mapper = mock(WechatUserBindingMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        AccountEligibilityService eligibility = mock(AccountEligibilityService.class);
        when(client.exchangeSession("code")).thenReturn(new WechatMiniappClient.WechatCodeSession("openid", "session", "unionid"));
        when(mapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        WechatAuthService service = new WechatAuthService(client, mapper, userMapper, eligibility);

        WechatSessionVO result = service.exchangeSession("code");

        assertEquals("openid", result.getOpenid());
        assertTrue(result.isSessionEstablished());
        assertFalse(result.isBound());
        verify(mapper).insert(any(WechatUserBindingDO.class));
    }

    @Test
    void returnsBoundStateForActiveBinding() {
        WechatMiniappClient client = mock(WechatMiniappClient.class);
        WechatUserBindingMapper mapper = mock(WechatUserBindingMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        AccountEligibilityService eligibility = mock(AccountEligibilityService.class);
        when(client.exchangeSession("code")).thenReturn(new WechatMiniappClient.WechatCodeSession("openid", "session", "unionid"));
        when(mapper.selectOne(any(QueryWrapper.class))).thenReturn(activeBinding());
        when(userMapper.selectById(10L)).thenReturn(activeUser());
        when(eligibility.isUserEligible(any(SysUserDO.class))).thenReturn(true);
        when(eligibility.isEmployeeActive(20L)).thenReturn(true);
        WechatAuthService service = new WechatAuthService(client, mapper, userMapper, eligibility);

        WechatSessionVO result = service.exchangeSession("code");

        assertEquals("openid", result.getOpenid());
        assertTrue(result.isSessionEstablished());
        assertTrue(result.isBound());
        assertEquals(10L, result.getUserId());
        assertEquals(20L, result.getEmployeeId());
        verify(mapper).updateById(any(WechatUserBindingDO.class));
    }

    @Test
    void returnsUnboundStateWhenEmployeeIsInactive() {
        WechatMiniappClient client = mock(WechatMiniappClient.class);
        WechatUserBindingMapper mapper = mock(WechatUserBindingMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        AccountEligibilityService eligibility = mock(AccountEligibilityService.class);
        when(client.exchangeSession("code")).thenReturn(new WechatMiniappClient.WechatCodeSession("openid", "session", "unionid"));
        when(mapper.selectOne(any(QueryWrapper.class))).thenReturn(activeBinding());
        when(userMapper.selectById(10L)).thenReturn(activeUser());
        when(eligibility.isUserEligible(any(SysUserDO.class))).thenReturn(true);
        when(eligibility.isEmployeeActive(20L)).thenReturn(false);
        WechatAuthService service = new WechatAuthService(client, mapper, userMapper, eligibility);

        WechatSessionVO result = service.exchangeSession("code");

        assertFalse(result.isBound());
    }

    private SysUserDO activeUser() {
        SysUserDO user = new SysUserDO();
        user.setId(10L);
        user.setEmployeeId(20L);
        user.setStatus("active");
        user.setDeleted(0);
        return user;
    }

    private WechatUserBindingDO activeBinding() {
        WechatUserBindingDO binding = new WechatUserBindingDO();
        binding.setId(1L);
        binding.setOpenid("openid");
        binding.setUnionid("old-unionid");
        binding.setStatus("active");
        binding.setUserId(10L);
        binding.setEmployeeId(20L);
        binding.setNickname("员工");
        return binding;
    }
}
