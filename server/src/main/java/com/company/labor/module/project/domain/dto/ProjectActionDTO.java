package com.company.labor.module.project.domain.dto;

import lombok.Data;

@Data
public class ProjectActionDTO {
    private String status;
    private String remark;
    private Long operatorId;
    private String operatorName;
}
