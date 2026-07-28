package com.company.labor.module.compensation.domain.vo;

import com.company.labor.module.compensation.domain.entity.PerformanceScoreDO;
import com.company.labor.module.compensation.domain.entity.PerformanceScoreItemDO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PerformanceScoreDetailVO {
    private PerformanceScoreDO score;
    private List<PerformanceScoreItemDO> items;
}
