package com.company.labor.module.salary.domain.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class SalaryProfileSaveDTO {
    private Long employeeId;
    private LocalDate effectiveDate;
    private String status;
    private String remark;
    private List<SalaryProfileItemDTO> items;
}

