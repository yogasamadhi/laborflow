package com.company.labor.module.employee.domain.vo;

import com.company.labor.module.employee.domain.entity.DepartmentDO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DepartmentReferenceVO {
    private Long id;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private String status;

    public static DepartmentReferenceVO from(DepartmentDO department) {
        return new DepartmentReferenceVO(
                department.getId(),
                department.getName(),
                department.getParentId(),
                department.getSortOrder(),
                department.getStatus());
    }
}
