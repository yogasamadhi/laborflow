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
@TableName("performance_score")
public class PerformanceScoreDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long performancePlanId;
    private String performanceMonth;
    private Long projectId;
    private Long employeeId;
    private Long positionId;
    private BigDecimal score;
    private BigDecimal performanceAmount;
    private String autoMetricSnapshot;
    private String status;
    private String remark;
}
