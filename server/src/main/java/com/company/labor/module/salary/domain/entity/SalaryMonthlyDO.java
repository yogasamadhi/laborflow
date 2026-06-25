package com.company.labor.module.salary.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("salary_monthly")
public class SalaryMonthlyDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String salaryMonth;
    private String title;
    private Integer totalEmployeeCount;
    private BigDecimal totalPayableAmount;
    private BigDecimal totalDeductionAmount;
    private BigDecimal totalActualAmount;
    private String status;
    private Long createdBy;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;
    private String remark;
    private Integer deleted;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}

