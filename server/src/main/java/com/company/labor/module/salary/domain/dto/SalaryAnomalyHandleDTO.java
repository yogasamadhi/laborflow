package com.company.labor.module.salary.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SalaryAnomalyHandleDTO {
    @NotBlank(message = "异常标识必填")
    private String anomalyKey;
    @NotBlank(message = "处理状态必填")
    @Pattern(regexp = "handled|ignored|adjustment", message = "处理状态不合法")
    private String status;
    @Size(max = 500, message = "处理备注不能超过 500 字")
    private String remark;
}
