package com.company.labor.module.project.domain.vo;

import com.company.labor.module.project.domain.entity.VariationOrderConfirmDO;
import com.company.labor.module.project.domain.entity.VariationOrderDO;
import java.util.List;
import lombok.Data;

@Data
public class VariationOrderDetailVO {
    private VariationOrderDO order;
    private String projectName;
    private String sectionName;
    private List<VariationOrderConfirmDO> confirms;
    private List<BusinessFileVO> files;
}
