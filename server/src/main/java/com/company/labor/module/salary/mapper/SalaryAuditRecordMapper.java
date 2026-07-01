package com.company.labor.module.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.salary.domain.entity.SalaryAuditRecordDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SalaryAuditRecordMapper extends BaseMapper<SalaryAuditRecordDO> {
    @Select("""
        SELECT *
        FROM salary_audit_record
        WHERE salary_monthly_id = #{monthlyId}
        ORDER BY created_at DESC, id DESC
        """)
    List<SalaryAuditRecordDO> selectByMonthlyId(@Param("monthlyId") Long monthlyId);
}
