package com.company.labor.module.salary.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private String remark;
    private List<SalaryDetailItemVO> earningItems;
    private List<SalaryDetailItemVO> deductionItems;
}
