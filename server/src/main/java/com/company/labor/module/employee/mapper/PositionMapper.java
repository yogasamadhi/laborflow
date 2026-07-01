package com.company.labor.module.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.employee.domain.entity.PositionDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface PositionMapper extends BaseMapper<PositionDO> {
    @Select("SELECT * FROM position WHERE id = #{id}")
    PositionDO selectByIdIncludingDeleted(@Param("id") Long id);

    @Select("""
            SELECT COUNT(1)
            FROM position
            WHERE name = #{name}
              AND deleted = 0
              AND id <> #{id}
              AND ((department_id IS NULL AND #{departmentId} IS NULL) OR department_id = #{departmentId})
            """)
    long countActiveByNameAndDepartmentExceptId(@Param("name") String name, @Param("departmentId") Long departmentId, @Param("id") Long id);

    @Update("UPDATE position SET deleted = 0, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
