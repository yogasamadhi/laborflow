package com.company.labor.module.compensation.domain.dto;

import java.math.BigDecimal;
import lombok.Data;

public final class CompensationWriteDTO {
    private CompensationWriteDTO() {
    }

    @Data
    public static class AttendanceRecordRequest {
        private Long employeeId;
        private String attendanceMonth;
        private Long projectId;
        private BigDecimal workDays;
        private BigDecimal leaveDays;
        private BigDecimal overtimeDays;
        private BigDecimal absentDays;
        private BigDecimal fieldDays;
        private BigDecimal projectSiteDays;
        private BigDecimal deductionAmount;
        private String remark;
    }

    @Data
    public static class PerformanceIndicatorRequest {
        private Long positionId;
        private String name;
        private String category;
        private BigDecimal scoreWeight;
        private String dataSource;
        private String status;
        private String remark;
    }

    @Data
    public static class PerformancePlanRequest {
        private String name;
        private String performanceMonth;
        private Long projectId;
        private Long positionId;
        private String status;
        private String remark;
    }

    @Data
    public static class PerformanceScoreRequest {
        private Long performancePlanId;
        private String performanceMonth;
        private Long projectId;
        private Long employeeId;
        private Long positionId;
        private BigDecimal score;
        private BigDecimal performanceAmount;
        private String status;
        private String remark;
    }

    @Data
    public static class ProjectBonusPoolRequest {
        private Long projectId;
        private String bonusMonth;
        private BigDecimal finalGrossProfit;
        private BigDecimal bonusRatio;
        private String releaseCondition;
        private String releaseStatus;
        private String remark;
    }

    @Data
    public static class ProjectBonusAllocationRequest {
        private Long bonusPoolId;
        private Long employeeId;
        private String roleName;
        private BigDecimal allocationRatio;
        private BigDecimal allocationAmount;
        private String paymentMonth;
        private String status;
        private String remark;
    }
}
