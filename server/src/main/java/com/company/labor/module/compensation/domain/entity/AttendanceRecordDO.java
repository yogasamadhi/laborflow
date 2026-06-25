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
@TableName("attendance_record")
public class AttendanceRecordDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long employeeId;
    private String attendanceMonth;
    private Long projectId;
    private BigDecimal workDays;
    private BigDecimal leaveDays;
    private BigDecimal overtimeDays;
    private BigDecimal absentDays;
    private BigDecimal fieldDays;
    private BigDecimal projectSiteDays;
    private BigDecimal deductionAmount;
    private String remark;
}
