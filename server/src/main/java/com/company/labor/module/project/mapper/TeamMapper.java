package com.company.labor.module.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.project.domain.entity.TeamDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface TeamMapper extends BaseMapper<TeamDO> {
    @Select("SELECT * FROM team WHERE id = #{id}")
    TeamDO selectByIdIncludingDeleted(@Param("id") Long id);

    @Select("""
            SELECT COUNT(1)
            FROM team
            WHERE name = #{name}
              AND deleted = 0
              AND id <> #{id}
            """)
    long countActiveByNameExceptId(@Param("name") String name, @Param("id") Long id);

    @Update("UPDATE team SET deleted = 0, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
