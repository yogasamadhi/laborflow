package com.company.labor.module.salary.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("salary_monthly_detail")
public class SalaryMonthlyDetailDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long salaryMonthlyId;
    private Long employeeId;
    private Long departmentId;
    private Long positionId;
    private Long projectId;
    private BigDecimal payableAmount;
    private BigDecimal deductionAmount;
    private BigDecimal actualAmount;
    private String paymentStatus;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String paymentProofUrl;
    private String payslipStatus;
    private LocalDateTime payslipConfirmedAt;
    private String payslipFeedback;
    private LocalDateTime payslipFeedbackAt;
    private String remark;
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
