package com.company.labor.module.salary.domain.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SalaryYearSummaryVO {
    private String year;
    private BigDecimal payableAmount;
    private BigDecimal deductionAmount;
    private BigDecimal actualAmount;
    private long paidCount;
    private long unpaidCount;
}
