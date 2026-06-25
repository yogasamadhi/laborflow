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
@TableName("project_bonus_allocation")
public class ProjectBonusAllocationDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long bonusPoolId;
    private Long projectId;
    private Long employeeId;
    private String roleName;
    private BigDecimal allocationRatio;
    private BigDecimal allocationAmount;
    private String paymentMonth;
    private String status;
    private String remark;
}
