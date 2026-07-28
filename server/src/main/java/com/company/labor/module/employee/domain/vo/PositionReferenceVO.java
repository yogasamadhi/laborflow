package com.company.labor.module.employee.domain.vo;

import com.company.labor.module.employee.domain.entity.PositionDO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PositionReferenceVO {
    private Long id;
    private String name;
    private Long departmentId;
    private String status;

    public static PositionReferenceVO from(PositionDO position) {
        return new PositionReferenceVO(
                position.getId(),
                position.getName(),
                position.getDepartmentId(),
                position.getStatus());
    }
}
