package com.company.labor.module.salary.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("salary_profile_item")
public class SalaryProfileItemDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long salaryProfileId;
    private Long salaryItemId;
    private String salaryItemName;
    private String salaryItemType;
    private BigDecimal amount;
    private String remark;
}

