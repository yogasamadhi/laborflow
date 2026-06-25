package com.company.labor.module.salary.domain.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class SalaryPaymentDTO {
    private LocalDate paymentDate;
    private String paymentMethod;
    private String paymentProofUrl;
}

