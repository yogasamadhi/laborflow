package com.company.labor.module.salary.domain.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SalaryDetailItemVO {
    private String name;
    private String type;
    private BigDecimal amount;
    private String remark;
}
