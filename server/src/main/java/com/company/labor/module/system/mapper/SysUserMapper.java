package com.company.labor.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.system.domain.entity.SysUserDO;
import org.apache.ibatis.annotations.Select;

public interface SysUserMapper extends BaseMapper<SysUserDO> {
    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM sys_user u
                JOIN sys_user_role ur ON ur.user_id = u.id
                JOIN sys_role r ON r.id = ur.role_id
                WHERE u.deleted = 0
                  AND u.status = 'active'
                  AND r.deleted = 0
                  AND r.status = 'active'
                  AND r.code = 'ADMIN'
            )
            """)
    boolean hasActiveAdministrator();
}
