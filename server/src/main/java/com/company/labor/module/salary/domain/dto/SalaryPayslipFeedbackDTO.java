package com.company.labor.module.salary.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SalaryPayslipFeedbackDTO {
    @NotBlank(message = "异常反馈必填")
    @Size(max = 500, message = "异常反馈不能超过 500 字")
    private String feedback;
}
