package com.company.labor.module.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.project.domain.entity.ProjectDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ProjectMapper extends BaseMapper<ProjectDO> {
    @Select("SELECT * FROM project WHERE id = #{id}")
    ProjectDO selectByIdIncludingDeleted(@Param("id") Long id);

    @Update("UPDATE project SET deleted = 0, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
