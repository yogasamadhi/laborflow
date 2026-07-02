package com.company.labor.module.salary.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.labor.module.common.domain.entity.BaseDO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("salary_anomaly_record")
public class SalaryAnomalyRecordDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String anomalyKey;
    private String status;
    private Long handlerId;
    private String handlerName;
    private LocalDateTime handledAt;
    private String remark;
}
