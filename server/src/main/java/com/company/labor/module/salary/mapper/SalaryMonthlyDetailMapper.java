package com.company.labor.module.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDetailDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SalaryMonthlyDetailMapper extends BaseMapper<SalaryMonthlyDetailDO> {
    @Select("""
        SELECT monthly.*, COALESCE(department.name, '历史全公司') AS department_name
        FROM salary_monthly_detail detail
        JOIN salary_monthly monthly ON monthly.id = detail.salary_monthly_id
        LEFT JOIN department ON department.id = monthly.department_id
        WHERE detail.deleted = 0
          AND monthly.deleted = 0
          AND monthly.salary_month = #{salaryMonth}
          AND detail.employee_id = #{employeeId}
          AND monthly.id != #{monthlyId}
        ORDER BY monthly.id
        LIMIT 1
        """)
    SalaryMonthlyDO selectActiveMonthlyByEmployeeAndMonthExcludingMonthly(
            @Param("employeeId") Long employeeId,
            @Param("salaryMonth") String salaryMonth,
            @Param("monthlyId") Long monthlyId);
}
