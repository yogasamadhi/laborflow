package com.company.labor.module.compensation.domain.vo;

import com.company.labor.module.compensation.domain.entity.PerformancePlanDO;
import com.company.labor.module.compensation.domain.entity.PerformancePlanIndicatorDO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PerformancePlanDetailVO {
    private PerformancePlanDO plan;
    private List<PerformancePlanIndicatorDO> indicators;
}
