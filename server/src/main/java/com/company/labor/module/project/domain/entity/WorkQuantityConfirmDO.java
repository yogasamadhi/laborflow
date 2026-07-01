package com.company.labor.module.project.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("work_quantity_confirm")
public class WorkQuantityConfirmDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long dailyWorkReportId;
    private String confirmStage;
    private String confirmStatus;
    private Long confirmerId;
    private String confirmerName;
    private String statusBefore;
    private String statusAfter;
    private String remark;
    private LocalDateTime createdAt;
}
