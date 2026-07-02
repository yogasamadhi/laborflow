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
    private BigDecimal revenueAmount;
    private BigDecimal teamPayableAmount;
    private BigDecimal teamPaidAmount;
    private BigDecimal salaryAllocatedCostAmount;
    private BigDecimal otherCostAmount;
    private BigDecimal machineryCostAmount;
    private BigDecimal materialCostAmount;
    private BigDecimal totalCostAmount;
    private BigDecimal grossProfit;
    private BigDecimal profitRate;
    private BigDecimal costRate;
    private BigDecimal receivableAmount;
    private BigDecimal invoicedAmount;
    private BigDecimal confirmedVariationAmount;
    private BigDecimal pendingVariationAmount;
    private BigDecimal receivedAmount;
    private BigDecimal cashInAmount;
    private BigDecimal cashOutAmount;
    private BigDecimal cashBalanceAmount;
    private BigDecimal retentionAmount;
    private BigDecimal retentionReturnedAmount;
    private BigDecimal retentionBalanceAmount;
    private BigDecimal paymentProgress;
    private List<ProjectFinanceBreakdownVO> revenueBreakdowns;
    private List<ProjectFinanceBreakdownVO> costBreakdowns;
    private List<ProjectFinanceBreakdownVO> cashFlowBreakdowns;
    private List<ProjectRiskVO> risks;
}
