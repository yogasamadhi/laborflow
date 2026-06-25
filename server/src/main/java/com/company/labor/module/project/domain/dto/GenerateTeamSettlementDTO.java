package com.company.labor.module.project.domain.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class GenerateTeamSettlementDTO {
    private Long projectId;
    private Long teamId;
    private String settlementMonth;
    private BigDecimal compensationAmount;
    private BigDecimal deductionAmount;
    private String remark;
}
