package com.company.labor.module.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.project.domain.entity.ProjectSectionDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ProjectSectionMapper extends BaseMapper<ProjectSectionDO> {
    @Select("SELECT * FROM project_section WHERE id = #{id}")
    ProjectSectionDO selectByIdIncludingDeleted(@Param("id") Long id);

    @Select("""
            SELECT COUNT(1)
            FROM project_section
            WHERE project_id = #{projectId}
              AND name = #{name}
              AND deleted = 0
              AND id <> #{id}
            """)
    long countActiveByProjectAndNameExceptId(@Param("projectId") Long projectId, @Param("name") String name, @Param("id") Long id);

    @Update("UPDATE project_section SET deleted = 0, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
