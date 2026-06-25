package com.company.labor.module.compensation.domain.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CompensationDashboardVO {
    private String month;
    private BigDecimal attendanceDeductionAmount;
    private BigDecimal performanceAmount;
    private BigDecimal projectBonusAmount;
    private BigDecimal salaryActualAmount;
    private long scoredEmployeeCount;
    private long confirmedScoreCount;
    private long bonusAllocationCount;
}
