package com.company.labor.module.salary.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class SalaryProfileSaveDTO {
    @NotNull(message = "员工必填")
    private Long employeeId;
    private LocalDate effectiveDate;
    @Pattern(regexp = "active|inactive", message = "工资档案状态不合法")
    private String status;
    private String remark;
    @Valid
    @NotEmpty(message = "至少添加一个工资项")
    private List<SalaryProfileItemDTO> items;
}
