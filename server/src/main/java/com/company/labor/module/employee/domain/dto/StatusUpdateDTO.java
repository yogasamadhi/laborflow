package com.company.labor.module.employee.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StatusUpdateDTO {
    @NotBlank
    private String status;
}

