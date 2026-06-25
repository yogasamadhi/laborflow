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
@TableName("variation_order")
public class VariationOrderDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
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
    private String status;
    private String rejectedReason;
}
