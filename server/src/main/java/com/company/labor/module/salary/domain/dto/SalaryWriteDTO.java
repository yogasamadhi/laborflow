package com.company.labor.module.salary.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.Data;

public final class SalaryWriteDTO {
    private SalaryWriteDTO() {
    }

    @Data
    public static class SalaryItemRequest {
        @NotBlank(message = "工资项名称必填")
        private String name;
        @NotBlank(message = "工资项类型必填")
        @Pattern(regexp = "earning|deduction", message = "工资项类型只能为 earning 或 deduction")
        private String type;
        private Integer isFixed;
        private Integer sortOrder;
        @Pattern(regexp = "active|inactive", message = "工资项状态不合法")
        private String status;
        private String remark;
    }

    @Data
    public static class SalaryMonthlyRequest {
        @NotBlank(message = "工资月份必填")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "工资月份格式必须为 yyyy-MM")
        private String salaryMonth;
        private String title;
        @Min(value = 0, message = "固定工资规则只能为 0 或 1")
        @Max(value = 1, message = "固定工资规则只能为 0 或 1")
        private Integer includeFixedSalary;
        @Min(value = 0, message = "考勤扣款规则只能为 0 或 1")
        @Max(value = 1, message = "考勤扣款规则只能为 0 或 1")
        private Integer includeAttendanceDeduction;
        @Min(value = 0, message = "绩效工资规则只能为 0 或 1")
        @Max(value = 1, message = "绩效工资规则只能为 0 或 1")
        private Integer includePerformanceSalary;
        @Min(value = 0, message = "项目奖金规则只能为 0 或 1")
        @Max(value = 1, message = "项目奖金规则只能为 0 或 1")
        private Integer includeProjectBonus;
        private String remark;
    }

    @Data
    public static class SalaryMonthlyDetailRequest {
        @DecimalMin(value = "0.00", message = "应发金额不能小于 0")
        private BigDecimal payableAmount;
        @DecimalMin(value = "0.00", message = "扣款金额不能小于 0")
        private BigDecimal deductionAmount;
        @DecimalMin(value = "0.00", message = "实发金额不能小于 0")
        private BigDecimal actualAmount;
        private String remark;
    }
}
