package com.company.labor.module.project.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.labor.module.common.domain.entity.BaseDO;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("machinery_work_record")
public class MachineryWorkRecordDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private Long projectSectionId;
    private Long machineryEquipmentId;
    private LocalDate workDate;
    private BigDecimal shiftCount;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String idleReason;
    private String operatorName;
    private String remark;
}
