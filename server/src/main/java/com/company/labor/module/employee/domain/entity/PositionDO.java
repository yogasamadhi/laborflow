package com.company.labor.module.employee.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.labor.module.common.domain.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("position")
public class PositionDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private Long departmentId;
    private Integer sortOrder;
    private String status;
}

