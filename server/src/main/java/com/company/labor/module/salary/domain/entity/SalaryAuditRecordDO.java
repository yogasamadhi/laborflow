package com.company.labor.module.salary.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("salary_audit_record")
public class SalaryAuditRecordDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long salaryMonthlyId;
    private Long salaryDetailId;
    private String action;
    private Long operatorId;
    private String operatorName;
    private String statusBefore;
    private String statusAfter;
    private String remark;
    private String proofUrl;
    private LocalDateTime createdAt;
}
