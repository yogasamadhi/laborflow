package com.company.labor.module.project.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectRiskVO {
    private String type;
    private String level;
    private String message;
}
