package com.company.labor.module.project.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

public final class ProjectWriteDTO {
    private ProjectWriteDTO() {
    }

    @Data
    public static class ProjectRequest {
        @NotBlank(message = "项目名称必填")
        private String name;
        private String clientName;
        @DecimalMin(value = "0.00", message = "合同金额不能小于 0")
        private BigDecimal contractAmount;
        private LocalDate contractStartDate;
        private LocalDate contractEndDate;
        private String projectManager;
        private String address;
        private String scope;
        @Pattern(regexp = "not_started|in_progress|paused|completed|settled", message = "项目状态不合法")
        private String status;
        private String remark;
    }

    @Data
    public static class SectionRequest {
        @NotNull(message = "项目必填")
        private Long projectId;
        private String bidSectionName;
        @NotBlank(message = "施工段名称必填")
        private String name;
        private String startStakeNo;
        private String endStakeNo;
        private String startWellNo;
        private String endWellNo;
        @DecimalMin(value = "0.00", message = "施工段长度不能小于 0")
        private BigDecimal length;
        private String remark;
    }

    @Data
    public static class WorkProcessRequest {
        @NotBlank(message = "工序名称必填")
        private String name;
        @NotBlank(message = "单位必填")
        private String unit;
        @Min(value = 0, message = "排序不能小于 0")
        private Integer sortOrder;
        @Pattern(regexp = "active|inactive", message = "工序状态不合法")
        private String status;
        private String remark;
    }

    @Data
    public static class TeamRequest {
        @NotBlank(message = "班组名称必填")
        private String name;
        private String leaderName;
        @Pattern(regexp = "^$|^1\\d{10}$", message = "班组联系电话格式不正确")
        private String phone;
        private String workType;
        private String settlementMethod;
        @Pattern(regexp = "active|inactive", message = "班组状态不合法")
        private String status;
        private String remark;
    }

    @Data
    public static class TeamUnitPriceRequest {
        @NotNull(message = "项目必填")
        private Long projectId;
        @NotNull(message = "班组必填")
        private Long teamId;
        @NotNull(message = "工序必填")
        private Long workProcessId;
        @NotBlank(message = "单位必填")
        private String unit;
        @DecimalMin(value = "0.00", message = "单价不能小于 0")
        private BigDecimal unitPrice;
        private LocalDate effectiveDate;
        @Pattern(regexp = "active|inactive", message = "单价状态不合法")
        private String status;
        private String remark;
    }

    @Data
    public static class ProjectProcessPriceRequest {
        @NotNull(message = "项目必填")
        private Long projectId;
        @NotNull(message = "工序必填")
        private Long workProcessId;
        @NotBlank(message = "单位必填")
        private String unit;
        @DecimalMin(value = "0.00", message = "单价不能小于 0")
        private BigDecimal unitPrice;
        private LocalDate effectiveDate;
        @Pattern(regexp = "active|inactive", message = "单价状态不合法")
        private String status;
        private String remark;
    }

    @Data
    public static class ProjectContractItemRequest {
        @NotNull(message = "项目必填")
        private Long projectId;
        private Long projectSectionId;
        @NotNull(message = "工序必填")
        private Long workProcessId;
        private String itemCode;
        @NotBlank(message = "清单名称必填")
        private String itemName;
        @NotBlank(message = "单位必填")
        private String unit;
        @DecimalMin(value = "0.00", message = "合同工程量不能小于 0")
        private BigDecimal contractQuantity;
        @DecimalMin(value = "0.00", message = "单价不能小于 0")
        private BigDecimal unitPrice;
        @Pattern(regexp = "active|inactive", message = "清单状态不合法")
        private String status;
        private String remark;
    }

    @Data
    public static class DailyWorkReportRequest {
        @NotNull(message = "施工日期必填")
        private LocalDate workDate;
        @NotNull(message = "项目必填")
        private Long projectId;
        @NotNull(message = "施工段必填")
        private Long projectSectionId;
        private Long contractItemId;
        @NotNull(message = "工序必填")
        private Long workProcessId;
        @DecimalMin(value = "0.00", message = "完成工程量不能小于 0")
        private BigDecimal completedQuantity;
        @NotBlank(message = "单位必填")
        private String unit;
        private Long teamId;
        @Min(value = 0, message = "施工人数不能小于 0")
        private Integer workerCount;
        private String machineryUsage;
        private String exceptionNote;
        private String reporterName;
    }

    @Data
    public static class VariationOrderRequest {
        @NotNull(message = "项目必填")
        private Long projectId;
        private Long projectSectionId;
        @NotNull(message = "发生日期必填")
        private LocalDate occurrenceDate;
        @NotBlank(message = "签证类型必填")
        private String variationType;
        private String reason;
        private String responsibleParty;
        @DecimalMin(value = "0.00", message = "增加工程量不能小于 0")
        private BigDecimal increasedQuantity;
        @DecimalMin(value = "0.00", message = "预计金额不能小于 0")
        private BigDecimal estimatedAmount;
        @Min(value = 0, message = "人工数量不能小于 0")
        private Integer laborCount;
        private String machineryShift;
        private String materialNote;
    }

    @Data
    public static class ProjectCostRecordRequest {
        @NotNull(message = "项目必填")
        private Long projectId;
        private LocalDate costDate;
        @NotBlank(message = "成本类型必填")
        private String costType;
        @DecimalMin(value = "0.00", message = "成本金额不能小于 0")
        private BigDecimal amount;
        private String description;
        private String remark;
    }

    @Data
    public static class ProjectPaymentRecordRequest {
        @NotNull(message = "项目必填")
        private Long projectId;
        @Pattern(regexp = "receivable|received", message = "回款记录类型不合法")
        private String recordType;
        private LocalDate paymentDate;
        @DecimalMin(value = "0.00", message = "回款金额不能小于 0")
        private BigDecimal amount;
        private String payerName;
        private Long voucherFileId;
        private String remark;
    }

    @Data
    public static class MachineryEquipmentRequest {
        @NotBlank(message = "机械设备名称必填")
        private String name;
        @NotBlank(message = "机械类型必填")
        private String equipmentType;
        private String ownerName;
        @DecimalMin(value = "0.00", message = "台班单价不能小于 0")
        private BigDecimal unitPrice;
        @Pattern(regexp = "active|inactive", message = "机械设备状态不合法")
        private String status;
        private String remark;
    }

    @Data
    public static class MachineryWorkRecordRequest {
        @NotNull(message = "项目必填")
        private Long projectId;
        private Long projectSectionId;
        @NotNull(message = "机械设备必填")
        private Long machineryEquipmentId;
        private LocalDate workDate;
        @DecimalMin(value = "0.00", message = "台班数量不能小于 0")
        private BigDecimal shiftCount;
        @DecimalMin(value = "0.00", message = "台班单价不能小于 0")
        private BigDecimal unitPrice;
        @DecimalMin(value = "0.00", message = "机械金额不能小于 0")
        private BigDecimal amount;
        private String idleReason;
        private String operatorName;
        private String remark;
    }

    @Data
    public static class MaterialRecordRequest {
        @NotNull(message = "项目必填")
        private Long projectId;
        private Long projectSectionId;
        @NotBlank(message = "材料名称必填")
        private String materialName;
        @NotBlank(message = "材料记录类型必填")
        @Pattern(regexp = "arrival|usage|impact", message = "材料记录类型不合法")
        private String recordType;
        private LocalDate recordDate;
        @DecimalMin(value = "0.00", message = "材料数量不能小于 0")
        private BigDecimal quantity;
        private String unit;
        @DecimalMin(value = "0.00", message = "材料单价不能小于 0")
        private BigDecimal unitPrice;
        @DecimalMin(value = "0.00", message = "材料金额不能小于 0")
        private BigDecimal amount;
        private String supplierName;
        private String impactNote;
        private String remark;
    }

    @Data
    public static class ProjectDocumentArchiveRequest {
        @NotNull(message = "项目必填")
        private Long projectId;
        private Long projectSectionId;
        private Long workProcessId;
        private LocalDate archiveDate;
        @NotBlank(message = "资料类型必填")
        private String documentType;
        @NotNull(message = "附件必填")
        private Long fileId;
        private String uploaderName;
        private String remark;
    }
}
