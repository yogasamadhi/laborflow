package com.company.labor.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.security.LoginUser;

public interface SysUserService extends IService<SysUserDO> {
    LoginUser loadLoginUserById(Long userId);
}

