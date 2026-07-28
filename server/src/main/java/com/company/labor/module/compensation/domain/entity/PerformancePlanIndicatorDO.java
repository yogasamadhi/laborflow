package com.company.labor.module.compensation.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("performance_plan_indicator")
public class PerformancePlanIndicatorDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long performancePlanId;
    private Long indicatorId;
    private String name;
    private String category;
    private String dataSource;
    private String metricCode;
    private BigDecimal scoreWeight;
    private String scoringDirection;
    private BigDecimal targetValue;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
