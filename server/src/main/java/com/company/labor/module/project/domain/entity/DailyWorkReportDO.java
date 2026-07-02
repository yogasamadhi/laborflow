package com.company.labor.module.project.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.labor.module.common.domain.entity.BaseDO;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_work_report")
public class DailyWorkReportDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private LocalDate workDate;
    private Long projectId;
    private Long projectSectionId;
    private Long contractItemId;
    private Long workProcessId;
    private BigDecimal completedQuantity;
    private String unit;
    private BigDecimal outputValue;
    private String pricingBasis;
    private Long teamId;
    private Integer workerCount;
    private String machineryUsage;
    private String exceptionNote;
    private String reporterName;
    private String status;
    private String rejectedReason;
}
