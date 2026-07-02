package com.company.labor.module.project.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.labor.module.common.domain.entity.BaseDO;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_contract_item")
public class ProjectContractItemDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private Long projectSectionId;
    private Long workProcessId;
    private String itemCode;
    private String itemName;
    private String unit;
    private BigDecimal contractQuantity;
    private BigDecimal unitPrice;
    private BigDecimal contractAmount;
    private String status;
    private String remark;
}
