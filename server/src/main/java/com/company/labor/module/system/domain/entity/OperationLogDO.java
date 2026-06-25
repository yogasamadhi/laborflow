package com.company.labor.module.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("operation_log")
public class OperationLogDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String module;
    private Long businessId;
    private String action;
    private Long operatorId;
    private String operatorName;
    private String content;
    private LocalDateTime createdAt;
}

