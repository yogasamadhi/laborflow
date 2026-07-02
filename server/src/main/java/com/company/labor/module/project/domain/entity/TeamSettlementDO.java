package com.company.labor.module.project.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.labor.module.common.domain.entity.BaseDO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("team_settlement")
public class TeamSettlementDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private Long teamId;
    private String projectName;
    private String teamName;
    private String settlementMonth;
    private BigDecimal confirmedQuantity;
    private BigDecimal settlementAmount;
    private BigDecimal compensationAmount;
    private BigDecimal deductionAmount;
    private BigDecimal actualPayAmount;
    private String settlementStatus;
    private String paymentStatus;
    private Long auditorId;
    private String auditorName;
    private LocalDateTime auditedAt;
    private String rejectedReason;
    private LocalDate paidDate;
    private Long paidBy;
    private String paidOperatorName;
    private String paymentRemark;
    private String remark;
}
