package com.company.labor.module.compensation.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
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
    public static class PerformanceIndicatorRequest {
        private Long positionId;
        @NotBlank(message = "指标名称必填")
        private String name;
        private String category;
        @DecimalMin(value = "0.00", message = "权重不能小于 0")
        private BigDecimal scoreWeight;
        @Pattern(regexp = "manual|project|attendance|salary", message = "数据来源不合法")
        private String dataSource;
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
        @Pattern(regexp = "draft|active|inactive|confirmed", message = "绩效方案状态不合法")
        private String status;
        private String remark;
    }

    @Data
    public static class PerformanceScoreRequest {
        private Long performancePlanId;
        @NotBlank(message = "绩效月份必填")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "绩效月份格式必须为 yyyy-MM")
        private String performanceMonth;
        private Long projectId;
        @NotNull(message = "员工必填")
        private Long employeeId;
        private Long positionId;
        @DecimalMin(value = "0.00", message = "绩效分数不能小于 0")
        private BigDecimal score;
        @DecimalMin(value = "0.00", message = "绩效金额不能小于 0")
        private BigDecimal performanceAmount;
        @Pattern(regexp = "draft|confirmed", message = "绩效评分状态不合法")
        private String status;
        private String remark;
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
