package com.company.labor.module.project.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("team_settlement_detail")
public class TeamSettlementDetailDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long teamSettlementId;
    private Long dailyWorkReportId;
    private Long workProcessId;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String remark;
}
