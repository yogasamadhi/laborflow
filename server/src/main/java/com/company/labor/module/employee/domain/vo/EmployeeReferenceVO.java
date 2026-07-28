package com.company.labor.module.employee.domain.vo;

import com.company.labor.module.employee.domain.entity.EmployeeDO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeeReferenceVO {
    private Long id;
    private String name;
    private Long departmentId;
    private Long positionId;
    private String status;

    public static EmployeeReferenceVO from(EmployeeDO employee) {
        return new EmployeeReferenceVO(
                employee.getId(),
                employee.getName(),
                employee.getDepartmentId(),
                employee.getPositionId(),
                employee.getStatus());
    }
}
