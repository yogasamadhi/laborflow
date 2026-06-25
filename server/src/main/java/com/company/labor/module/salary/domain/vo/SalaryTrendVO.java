package com.company.labor.module.salary.domain.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SalaryTrendVO {
    private String salaryMonth;
    private BigDecimal actualAmount;
}

