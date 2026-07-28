package com.company.labor.module.project.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class VariationConfirmDTO {
    @NotNull(message = "确认金额必填")
    @DecimalMin(value = "0.00", message = "确认金额不能小于 0")
    private BigDecimal confirmedAmount;
    private String remark;
    private Long operatorId;
    private String operatorName;
}
