package com.company.labor.module.project.domain.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ProjectActionDTO {
    @Pattern(regexp = "not_started|in_progress|paused|completed|settled|draft|submitted|pm_confirmed|confirmed|rejected|approved|paid|released|allocated|active|inactive", message = "状态值不合法")
    private String status;
    private String remark;
    private Long operatorId;
    private String operatorName;
}
