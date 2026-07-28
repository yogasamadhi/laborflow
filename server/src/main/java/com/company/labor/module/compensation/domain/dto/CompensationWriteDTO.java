package com.company.labor.module.compensation.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

public final class CompensationWriteDTO {
    private CompensationWriteDTO() {
    }

    @Data
    public static class AttendanceRecordRequest {
        @NotNull(message = "员工必填")
        private Long employeeId;
        @NotBlank(message = "考勤月份必填")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "考勤月份格式必须为 yyyy-MM")
        private String attendanceMonth;
        private Long projectId;
        @DecimalMin(value = "0.00", message = "出勤天数不能小于 0")
        private BigDecimal workDays;
        @DecimalMin(value = "0.00", message = "请假天数不能小于 0")
        private BigDecimal leaveDays;
        @DecimalMin(value = "0.00", message = "加班天数不能小于 0")
        private BigDecimal overtimeDays;
        @DecimalMin(value = "0.00", message = "缺勤天数不能小于 0")
        private BigDecimal absentDays;
        @DecimalMin(value = "0.00", message = "外勤天数不能小于 0")
        private BigDecimal fieldDays;
        @DecimalMin(value = "0.00", message = "驻场天数不能小于 0")
        private BigDecimal projectSiteDays;
        @DecimalMin(value = "0.00", message = "扣款金额不能小于 0")
        private BigDecimal deductionAmount;
        private String remark;
    }

    @Data
    public static class SalaryDeductionRecordRequest {
        @NotNull(message = "员工必填")
        private Long employeeId;
        @NotBlank(message = "扣款月份必填")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "扣款月份格式必须为 yyyy-MM")
        private String deductionMonth;
        @NotBlank(message = "扣款类型必填")
        @Pattern(regexp = "social_security|housing_fund|individual_tax|advance|other", message = "扣款类型不合法")
        private String deductionType;
        private Long salaryItemId;
        private String salaryItemName;
        @DecimalMin(value = "0.00", message = "扣款金额不能小于 0")
        private BigDecimal amount;
        @Pattern(regexp = "active|inactive", message = "扣款状态不合法")
        private String status;
        private String remark;
    }

    @Data
    public static class PerformanceIndicatorRequest {
        private Long positionId;
        @NotBlank(message = "指标名称必填")
        private String name;
        private String category;
        @DecimalMin(value = "0.00", message = "权重不能小于 0")
        private BigDecimal scoreWeight;
        @Pattern(regexp = "manual|project|attendance|salary", message = "数据来源不合法")
        private String dataSource;
        private String metricCode;
        @Pattern(regexp = "higher_better|lower_better", message = "评分方向不合法")
        private String scoringDirection;
        @DecimalMin(value = "0.00", message = "目标值不能小于 0")
        private BigDecimal targetValue;
        @Pattern(regexp = "active|inactive", message = "指标状态不合法")
        private String status;
        private String remark;
    }

    @Data
    public static class PerformancePlanRequest {
        @NotBlank(message = "绩效方案名称必填")
        private String name;
        @NotBlank(message = "绩效月份必填")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "绩效月份格式必须为 yyyy-MM")
        private String performanceMonth;
        private Long projectId;
        private Long positionId;
        @NotEmpty(message = "绩效方案至少选择一个指标")
        private List<Long> indicatorIds;
        private String remark;
    }

    @Data
    public static class PerformanceScoreGenerateRequest {
        @NotEmpty(message = "至少选择一名员工")
        private List<Long> employeeIds;
    }

    @Data
    public static class PerformanceScoreUpdateRequest {
        @NotEmpty(message = "绩效评分明细必填")
        private List<PerformanceScoreItemRequest> items;
        private String remark;
    }

    @Data
    public static class PerformanceScoreItemRequest {
        @NotNull(message = "方案指标必填")
        private Long planIndicatorId;
        @NotNull(message = "最终评分必填")
        @DecimalMin(value = "0.00", message = "最终评分不能小于 0")
        @DecimalMax(value = "100.00", message = "最终评分不能大于 100")
        private BigDecimal finalScore;
        private String overrideReason;
    }

    @Data
    public static class ProjectBonusPoolRequest {
        @NotNull(message = "项目必填")
        private Long projectId;
        @NotBlank(message = "奖金月份必填")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "奖金月份格式必须为 yyyy-MM")
        private String bonusMonth;
        @DecimalMin(value = "0.00", message = "最终毛利不能小于 0")
        private BigDecimal finalGrossProfit;
        @DecimalMin(value = "0.00", message = "奖金比例不能小于 0")
        private BigDecimal bonusRatio;
        private String releaseCondition;
        @Pattern(regexp = "draft|released", message = "奖金池状态不合法")
        private String releaseStatus;
        private String remark;
    }

    @Data
    public static class ProjectBonusAllocationRequest {
        @NotNull(message = "奖金池必填")
        private Long bonusPoolId;
        @NotNull(message = "员工必填")
        private Long employeeId;
        private String roleName;
        @DecimalMin(value = "0.00", message = "分配比例不能小于 0")
        private BigDecimal allocationRatio;
        @DecimalMin(value = "0.00", message = "分配金额不能小于 0")
        private BigDecimal allocationAmount;
        @NotBlank(message = "发放月份必填")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "发放月份格式必须为 yyyy-MM")
        private String paymentMonth;
        @Pattern(regexp = "allocated|confirmed", message = "奖金分配状态不合法")
        private String status;
        private String remark;
    }
}
