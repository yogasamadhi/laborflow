package com.company.labor.module.salary.domain.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class SalaryProfileItemDTO {
    private Long salaryItemId;
    private String salaryItemName;
    private String salaryItemType;
    private BigDecimal amount;
    private String remark;
}

