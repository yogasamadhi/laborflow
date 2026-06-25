package com.company.labor.module.project.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("daily_work_report_file")
public class DailyWorkReportFileDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long dailyWorkReportId;
    private Long fileId;
    private String fileType;
    private LocalDateTime createdAt;
}
