package com.company.labor.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.system.domain.entity.SysRoleDO;
import java.util.List;
import org.apache.ibatis.annotations.Select;

public interface SysRoleMapper extends BaseMapper<SysRoleDO> {
    @Select("""
        SELECT r.code
        FROM sys_role r
        JOIN sys_user_role ur ON ur.role_id = r.id
        WHERE ur.user_id = #{userId} AND r.deleted = 0 AND r.status = 'active'
        """)
    List<String> selectRoleCodesByUserId(Long userId);
}

