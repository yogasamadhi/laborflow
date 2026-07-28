package com.company.labor.module.system.domain.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class TodoVO {
    private String id;
    private String type;
    private Long businessId;
    private String title;
    private String description;
    private String status;
    private Long projectId;
    private String projectName;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime occurredAt;
    private List<String> actions;
}
