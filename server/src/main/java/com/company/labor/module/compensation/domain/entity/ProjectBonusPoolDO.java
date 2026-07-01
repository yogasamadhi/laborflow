package com.company.labor.module.compensation.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.labor.module.common.domain.entity.BaseDO;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_bonus_pool")
public class ProjectBonusPoolDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private String bonusMonth;
    private BigDecimal finalGrossProfit;
    private BigDecimal bonusRatio;
    private BigDecimal bonusPoolAmount;
    private String releaseCondition;
    private String releaseStatus;
    private String dashboardSnapshot;
    private String remark;
}
