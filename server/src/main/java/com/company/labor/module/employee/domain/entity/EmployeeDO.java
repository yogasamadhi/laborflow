package com.company.labor.module.employee.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.labor.module.common.domain.entity.BaseDO;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("employee")
public class EmployeeDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String phone;
    private String idCard;
    private String gender;
    private LocalDate birthDate;
    private Long departmentId;
    private Long positionId;
    private String employeeType;
    private LocalDate entryDate;
    private LocalDate leaveDate;
    private String bankAccount;
    private String bankName;
    private String emergencyContact;
    private String emergencyPhone;
    private String status;
    private String remark;
}

