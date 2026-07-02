package com.company.labor.module.salary.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SalaryAnomalyVO {
    private String anomalyKey;
    private String type;
    private String level;
    private String salaryMonth;
    private Long monthlyId;
    private Long detailId;
    private Long employeeId;
    private String employeeName;
    private BigDecimal actualAmount;
    private BigDecimal compareAmount;
    private String message;
    private String processStatus;
    private String processRemark;
    private String processedBy;
    private LocalDateTime processedAt;
}
