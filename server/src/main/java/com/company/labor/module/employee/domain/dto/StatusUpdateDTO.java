package com.company.labor.module.employee.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class StatusUpdateDTO {
    @NotBlank(message = "状态必填")
    @Pattern(regexp = "active|inactive|left", message = "员工状态不合法")
    private String status;
}
