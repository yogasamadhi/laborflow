package com.company.labor.module.salary.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("salary_detail_item")
public class SalaryDetailItemDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long salaryDetailId;
    private Long salaryItemId;
    private String salaryItemName;
    private String salaryItemType;
    private BigDecimal amount;
    private String remark;
}

