package com.company.labor.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.mapper.SysRoleMapper;
import com.company.labor.module.system.mapper.SysUserMapper;
import com.company.labor.module.system.service.SysUserService;
import com.company.labor.security.LoginUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUserDO> implements SysUserService, UserDetailsService {
    private final SysRoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUserDO user = getOne(new LambdaQueryWrapper<SysUserDO>().eq(SysUserDO::getUsername, username).eq(SysUserDO::getDeleted, 0), false);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return toLoginUser(user);
    }

    @Override
    public LoginUser loadLoginUserById(Long userId) {
        SysUserDO user = getById(userId);
        if (user == null || user.getDeleted() != null && user.getDeleted() == 1) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return toLoginUser(user);
    }

    private LoginUser toLoginUser(SysUserDO user) {
        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getId());
        return new LoginUser(user, roles);
    }
}

