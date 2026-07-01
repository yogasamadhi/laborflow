package com.company.labor.module.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.salary.domain.entity.SalaryItemDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SalaryItemMapper extends BaseMapper<SalaryItemDO> {
    @Select("SELECT * FROM salary_item WHERE id = #{id}")
    SalaryItemDO selectByIdIncludingDeleted(@Param("id") Long id);

    @Select("""
            SELECT COUNT(1)
            FROM salary_item
            WHERE name = #{name}
              AND type = #{type}
              AND deleted = 0
              AND id <> #{id}
            """)
    long countActiveByNameAndTypeExceptId(@Param("name") String name, @Param("type") String type, @Param("id") Long id);

    @Update("UPDATE salary_item SET deleted = 0, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
