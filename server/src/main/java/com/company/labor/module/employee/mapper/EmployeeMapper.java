package com.company.labor.module.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.employee.domain.entity.EmployeeDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface EmployeeMapper extends BaseMapper<EmployeeDO> {
    @Select("SELECT * FROM employee WHERE id = #{id}")
    EmployeeDO selectByIdIncludingDeleted(@Param("id") Long id);
}
