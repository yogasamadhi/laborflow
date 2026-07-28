package com.company.labor.module.project.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DailyReportPricingVO {
    private boolean priced;
    private boolean selectionRequired;
    private List<ContractItemCandidateVO> candidates;
    private Long selectedContractItemId;
    private String sourceType;
    private Long sourceId;
    private BigDecimal unitPrice;
    private BigDecimal outputValue;
    private String basis;

    @Data
    @AllArgsConstructor
    public static class ContractItemCandidateVO {
        private Long id;
        private String itemCode;
        private String itemName;
        private String unit;
        private BigDecimal unitPrice;
        private Long projectSectionId;
    }
}
