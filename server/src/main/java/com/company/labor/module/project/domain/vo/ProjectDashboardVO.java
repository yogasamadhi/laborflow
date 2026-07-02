package com.company.labor.module.project.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class ProjectDashboardVO {
    private Long projectId;
    private String projectName;
    private BigDecimal contractAmount;
    private BigDecimal completedOutputValue;
    private BigDecimal confirmedQuantity;
    private BigDecimal teamPayableAmount;
    private BigDecimal otherCostAmount;
    private BigDecimal machineryCostAmount;
    private BigDecimal materialCostAmount;
    private BigDecimal totalCostAmount;
    private BigDecimal grossProfit;
    private BigDecimal receivableAmount;
    private BigDecimal confirmedVariationAmount;
    private BigDecimal pendingVariationAmount;
    private BigDecimal receivedAmount;
    private BigDecimal paymentProgress;
    private List<ProjectRiskVO> risks;
}
