package com.company.labor.module.system.domain.vo;

import com.company.labor.module.compensation.domain.vo.CompensationDashboardVO;
import com.company.labor.module.salary.domain.vo.SalaryOverviewVO;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class DashboardOverviewVO {
    private String month;
    private CapabilitiesVO capabilities;
    private SalaryOverviewVO salary;
    private ProjectPortfolioVO projects;
    private CompensationOverviewVO compensation;
    private EmployeeHandoffVO employee;

    @Data
    public static class CapabilitiesVO {
        private boolean salary;
        private boolean projects;
        private boolean compensation;
        private boolean employeeHandoff;
    }

    @Data
    public static class ProjectPortfolioVO {
        private long projectCount;
        private long inProgressCount;
        private BigDecimal contractAmount;
        private BigDecimal revenueAmount;
        private BigDecimal totalCostAmount;
        private BigDecimal grossProfit;
        private BigDecimal receivableAmount;
        private BigDecimal receivedAmount;
        private long riskCount;
    }

    @Data
    public static class CompensationOverviewVO {
        private CompensationDashboardVO totals;
        private long activeProfileCount;
        private long pendingPerformancePlanCount;
        private long draftPerformanceScoreCount;
        private long draftBonusPoolCount;
    }

    @Data
    public static class EmployeeHandoffVO {
        private String message;
        private boolean employeeBound;
    }
}
