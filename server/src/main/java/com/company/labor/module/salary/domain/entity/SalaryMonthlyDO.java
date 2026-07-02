package com.company.labor.module.salary.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
    private Integer includeFixedSalary;
    private Integer includeAttendanceDeduction;
    private Integer includePerformanceSalary;
    private Integer includeProjectBonus;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;
    private String remark;
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
