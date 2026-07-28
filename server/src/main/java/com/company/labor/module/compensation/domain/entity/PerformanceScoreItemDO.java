package com.company.labor.module.compensation.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("performance_score_item")
public class PerformanceScoreItemDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long performanceScoreId;
    private Long planIndicatorId;
    private BigDecimal actualValue;
    private BigDecimal autoScore;
    private BigDecimal finalScore;
    private String overrideReason;
    private String evidenceSnapshot;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
