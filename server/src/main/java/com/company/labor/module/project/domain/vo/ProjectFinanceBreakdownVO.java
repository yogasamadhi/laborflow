package com.company.labor.module.project.domain.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectFinanceBreakdownVO {
    private String category;
    private BigDecimal amount;
    private BigDecimal ratio;
    private String source;
}
