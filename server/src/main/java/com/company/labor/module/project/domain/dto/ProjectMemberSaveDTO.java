package com.company.labor.module.project.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ProjectMemberSaveDTO {
    @NotNull(message = "项目必填")
    private Long projectId;
    private Long userId;
    private Long employeeId;
    private String roleName;
    @Pattern(regexp = "active|inactive", message = "项目成员状态不合法")
    private String status;
    private String remark;
}
