package com.company.labor.module.salary.domain.dto;

import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.Data;

@Data
public class SalaryPaymentDTO {
    private LocalDate paymentDate;
    @Pattern(regexp = "bank_transfer|cash|wechat|alipay|other", message = "发放方式不合法")
    private String paymentMethod;
    private String paymentProofUrl;
}
