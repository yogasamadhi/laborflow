package com.company.labor.module.salary.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.labor.module.common.domain.entity.BaseDO;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("salary_profile")
public class SalaryProfileDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long employeeId;
    private LocalDate effectiveDate;
    private String status;
    private String remark;
}

