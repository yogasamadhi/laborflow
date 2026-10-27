package com.company.labor.module.salary.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class SalaryProfileListVO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long departmentId;
    private String departmentName;
    private LocalDate effectiveDate;
    private String status;
    private String remark;
    private BigDecimal earningAmount;
    private BigDecimal deductionAmount;
    private BigDecimal netAmount;
    private Integer itemCount;
}
