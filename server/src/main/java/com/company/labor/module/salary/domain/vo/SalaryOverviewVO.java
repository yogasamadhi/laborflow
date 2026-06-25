package com.company.labor.module.salary.domain.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SalaryOverviewVO {
    private BigDecimal payableAmount;
    private BigDecimal deductionAmount;
    private BigDecimal actualAmount;
    private BigDecimal paidAmount;
    private BigDecimal unpaidAmount;
    private long paidCount;
    private long unpaidCount;
}

