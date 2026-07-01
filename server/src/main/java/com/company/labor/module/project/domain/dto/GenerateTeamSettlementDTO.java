package com.company.labor.module.project.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class GenerateTeamSettlementDTO {
    @NotNull(message = "项目必填")
    private Long projectId;
    @NotNull(message = "班组必填")
    private Long teamId;
    @NotBlank(message = "结算月份必填")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "结算月份格式必须为 yyyy-MM")
    private String settlementMonth;
    @DecimalMin(value = "0.00", message = "补偿金额不能小于 0")
    private BigDecimal compensationAmount;
    @DecimalMin(value = "0.00", message = "扣款金额不能小于 0")
    private BigDecimal deductionAmount;
    private String remark;
}
