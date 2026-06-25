package com.company.labor.module.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.salary.domain.entity.SalaryDetailItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SalaryDetailItemMapper extends BaseMapper<SalaryDetailItemDO> {
    @Select("""
        SELECT item.*
        FROM salary_detail_item item
        JOIN salary_monthly_detail detail ON detail.id = item.salary_detail_id
        WHERE detail.salary_monthly_id = #{monthlyId}
          AND detail.deleted = 0
        ORDER BY detail.employee_id ASC, item.id ASC
        """)
    List<SalaryDetailItemDO> selectByMonthlyId(@Param("monthlyId") Long monthlyId);
}
