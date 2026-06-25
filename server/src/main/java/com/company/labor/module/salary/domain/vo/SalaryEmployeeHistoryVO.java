package com.company.labor.module.salary.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class SalaryEmployeeHistoryVO {
    private Long employeeId;
    private String employeeName;
    private BigDecimal totalActualAmount;
    private SalaryHistoryRecordVO latestPaymentRecord;
    private List<SalaryHistoryRecordVO> records;
    private List<SalaryYearSummaryVO> yearlySummaries;
    private List<SalaryTrendVO> monthlyTrend;
}
