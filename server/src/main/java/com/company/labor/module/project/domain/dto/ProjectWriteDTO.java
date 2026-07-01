package com.company.labor.module.project.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

public final class ProjectWriteDTO {
    private ProjectWriteDTO() {
    }

    @Data
    public static class ProjectRequest {
        private String name;
        private String clientName;
        private BigDecimal contractAmount;
        private LocalDate contractStartDate;
        private LocalDate contractEndDate;
        private String projectManager;
        private String address;
        private String scope;
        private String status;
        private String remark;
    }

    @Data
    public static class SectionRequest {
        private Long projectId;
        private String bidSectionName;
        private String name;
        private String startStakeNo;
        private String endStakeNo;
        private String startWellNo;
        private String endWellNo;
        private BigDecimal length;
        private String remark;
    }

    @Data
    public static class WorkProcessRequest {
        private String name;
        private String unit;
        private Integer sortOrder;
        private String status;
        private String remark;
    }

    @Data
    public static class TeamRequest {
        private String name;
        private String leaderName;
        private String phone;
        private String workType;
        private String settlementMethod;
        private String status;
        private String remark;
    }

    @Data
    public static class TeamUnitPriceRequest {
        private Long projectId;
        private Long teamId;
        private Long workProcessId;
        private String unit;
        private BigDecimal unitPrice;
        private LocalDate effectiveDate;
        private String status;
        private String remark;
    }

    @Data
    public static class ProjectProcessPriceRequest {
        private Long projectId;
        private Long workProcessId;
        private String unit;
        private BigDecimal unitPrice;
        private LocalDate effectiveDate;
        private String status;
        private String remark;
    }

    @Data
    public static class DailyWorkReportRequest {
        private LocalDate workDate;
        private Long projectId;
        private Long projectSectionId;
        private Long workProcessId;
        private BigDecimal completedQuantity;
        private String unit;
        private Long teamId;
        private Integer workerCount;
        private String machineryUsage;
        private String exceptionNote;
        private String reporterName;
    }

    @Data
    public static class VariationOrderRequest {
        private Long projectId;
        private Long projectSectionId;
        private LocalDate occurrenceDate;
        private String variationType;
        private String reason;
        private String responsibleParty;
        private BigDecimal increasedQuantity;
        private BigDecimal estimatedAmount;
        private Integer laborCount;
        private String machineryShift;
        private String materialNote;
    }

    @Data
    public static class ProjectCostRecordRequest {
        private Long projectId;
        private LocalDate costDate;
        private String costType;
        private BigDecimal amount;
        private String description;
        private String remark;
    }

    @Data
    public static class ProjectPaymentRecordRequest {
        private Long projectId;
        private String recordType;
        private LocalDate paymentDate;
        private BigDecimal amount;
        private String payerName;
        private Long voucherFileId;
        private String remark;
    }

    @Data
    public static class MachineryEquipmentRequest {
        private String name;
        private String equipmentType;
        private String ownerName;
        private BigDecimal unitPrice;
        private String status;
        private String remark;
    }

    @Data
    public static class MachineryWorkRecordRequest {
        private Long projectId;
        private Long projectSectionId;
        private Long machineryEquipmentId;
        private LocalDate workDate;
        private BigDecimal shiftCount;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private String idleReason;
        private String operatorName;
        private String remark;
    }

    @Data
    public static class MaterialRecordRequest {
        private Long projectId;
        private Long projectSectionId;
        private String materialName;
        private String recordType;
        private LocalDate recordDate;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private String supplierName;
        private String impactNote;
        private String remark;
    }

    @Data
    public static class ProjectDocumentArchiveRequest {
        private Long projectId;
        private Long projectSectionId;
        private Long workProcessId;
        private LocalDate archiveDate;
        private String documentType;
        private Long fileId;
        private String uploaderName;
        private String remark;
    }
}
