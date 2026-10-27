package com.company.labor.module.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.employee.domain.entity.DepartmentDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface DepartmentMapper extends BaseMapper<DepartmentDO> {
    @Select("SELECT * FROM department WHERE id = #{id}")
    DepartmentDO selectByIdIncludingDeleted(@Param("id") Long id);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM department
            WHERE name = #{name}
              AND deleted = 0
              AND id &lt;&gt; #{id}
            <choose>
              <when test="parentId == null">
                AND parent_id IS NULL
              </when>
              <otherwise>
                AND parent_id = #{parentId}
              </otherwise>
            </choose>
            </script>
            """)
    long countActiveByNameAndParentExceptId(@Param("name") String name, @Param("parentId") Long parentId, @Param("id") Long id);

    @Update("UPDATE department SET deleted = 0, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
