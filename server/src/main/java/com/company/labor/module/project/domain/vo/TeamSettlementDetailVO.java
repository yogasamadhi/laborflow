package com.company.labor.module.project.domain.vo;

import com.company.labor.module.project.domain.entity.TeamSettlementDO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeamSettlementDetailVO {
    private TeamSettlementDO settlement;
    private List<com.company.labor.module.project.domain.entity.TeamSettlementDetailDO> details;
}
