package com.company.labor.module.compensation.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("salary_source_record")
public class SalarySourceRecordDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long salaryMonthlyId;
    private Long salaryDetailId;
    private Long employeeId;
    private String sourceType;
    private Long sourceId;
    private String sourceName;
    private BigDecimal amount;
    private String remark;
    private LocalDateTime createdAt;
}
