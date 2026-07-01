package com.company.labor.module.salary.domain.dto;

import java.math.BigDecimal;
import lombok.Data;

public final class SalaryWriteDTO {
    private SalaryWriteDTO() {
    }

    @Data
    public static class SalaryItemRequest {
        private String name;
        private String type;
        private Integer isFixed;
        private Integer sortOrder;
        private String status;
        private String remark;
    }

    @Data
    public static class SalaryMonthlyRequest {
        private String salaryMonth;
        private String title;
        private String remark;
    }

    @Data
    public static class SalaryMonthlyDetailRequest {
        private BigDecimal payableAmount;
        private BigDecimal deductionAmount;
        private BigDecimal actualAmount;
        private String remark;
    }
}
