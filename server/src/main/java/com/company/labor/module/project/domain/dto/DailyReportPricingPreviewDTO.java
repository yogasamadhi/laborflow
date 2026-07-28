package com.company.labor.module.project.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class DailyReportPricingPreviewDTO {
    @NotNull(message = "施工日期必填")
    private LocalDate workDate;
    @NotNull(message = "项目必填")
    private Long projectId;
    @NotNull(message = "施工段必填")
    private Long projectSectionId;
    private Long contractItemId;
    @NotNull(message = "工序必填")
    private Long workProcessId;
    @NotNull(message = "完成工程量必填")
    @DecimalMin(value = "0.00", message = "完成工程量不能小于 0")
    private BigDecimal completedQuantity;
    @NotBlank(message = "单位必填")
    private String unit;
}
