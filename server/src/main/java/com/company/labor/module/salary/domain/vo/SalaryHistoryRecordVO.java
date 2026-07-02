package com.company.labor.module.salary.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SalaryHistoryRecordVO {
    private String salaryMonth;
    private Long monthlyId;
    private Long detailId;
    private BigDecimal payableAmount;
    private BigDecimal deductionAmount;
    private BigDecimal actualAmount;
    private String paymentStatus;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String payslipStatus;
    private LocalDateTime payslipConfirmedAt;
    private String payslipFeedback;
    private LocalDateTime payslipFeedbackAt;
    private String remark;
    private List<SalaryDetailItemVO> earningItems;
    private List<SalaryDetailItemVO> deductionItems;
}
