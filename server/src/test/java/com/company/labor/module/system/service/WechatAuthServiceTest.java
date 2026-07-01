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
import com.company.labor.module.system.domain.vo.WechatSessionVO;
import com.company.labor.module.system.mapper.WechatUserBindingMapper;
import org.junit.jupiter.api.Test;

class WechatAuthServiceTest {
    @Test
    void createsPendingBindingForNewOpenid() {
        WechatMiniappClient client = mock(WechatMiniappClient.class);
        WechatUserBindingMapper mapper = mock(WechatUserBindingMapper.class);
        when(client.exchangeSession("code")).thenReturn(new WechatMiniappClient.WechatCodeSession("openid", "session", "unionid"));
        when(mapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        WechatAuthService service = new WechatAuthService(client, mapper);

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
        when(client.exchangeSession("code")).thenReturn(new WechatMiniappClient.WechatCodeSession("openid", "session", "unionid"));
        when(mapper.selectOne(any(QueryWrapper.class))).thenReturn(activeBinding());
        WechatAuthService service = new WechatAuthService(client, mapper);

        WechatSessionVO result = service.exchangeSession("code");

        assertEquals("openid", result.getOpenid());
        assertTrue(result.isSessionEstablished());
        assertTrue(result.isBound());
        assertEquals(10L, result.getUserId());
        assertEquals(20L, result.getEmployeeId());
        verify(mapper).updateById(any(WechatUserBindingDO.class));
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
