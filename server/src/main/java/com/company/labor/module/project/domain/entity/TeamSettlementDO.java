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
@TableName("team_settlement")
public class TeamSettlementDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private Long teamId;
    private String settlementMonth;
    private BigDecimal confirmedQuantity;
    private BigDecimal settlementAmount;
    private BigDecimal compensationAmount;
    private BigDecimal deductionAmount;
    private BigDecimal actualPayAmount;
    private String settlementStatus;
    private String paymentStatus;
    private LocalDate paidDate;
    private String remark;
}
