package com.company.labor.module.salary.domain.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BatchSalaryPaymentDTO extends SalaryPaymentDTO {
    private Long monthlyId;
    private List<Long> detailIds;
}
