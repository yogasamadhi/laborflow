package com.company.labor.module.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.employee.domain.entity.EmployeeDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface EmployeeMapper extends BaseMapper<EmployeeDO> {
    @Select("SELECT * FROM employee WHERE id = #{id}")
    EmployeeDO selectByIdIncludingDeleted(@Param("id") Long id);

    @Select("""
            SELECT COUNT(1)
            FROM employee
            WHERE phone = #{phone}
              AND deleted = 0
              AND id <> #{id}
            """)
    long countActiveByPhoneExceptId(@Param("phone") String phone, @Param("id") Long id);

    @Update("UPDATE employee SET deleted = 0, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
