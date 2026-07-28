package com.company.labor.module.project.domain.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProjectMemberVO {
    private Long id;
    private Long projectId;
    private Long employeeId;
    private String employeeName;
    private Long positionId;
    private String positionName;
    private String employeeStatus;
    private String roleName;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime joinedAt;
}
