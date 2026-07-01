package com.company.labor.module.salary.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class SalaryProfileItemDTO {
    private Long salaryItemId;
    @NotBlank(message = "工资项名称必填")
    private String salaryItemName;
    @NotBlank(message = "工资项类型必填")
    @Pattern(regexp = "earning|deduction", message = "工资项类型只能为 earning 或 deduction")
    private String salaryItemType;
    @DecimalMin(value = "0.00", message = "工资项金额不能小于 0")
    private BigDecimal amount;
    private String remark;
}
