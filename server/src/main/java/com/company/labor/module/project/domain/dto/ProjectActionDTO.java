package com.company.labor.module.project.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ProjectActionDTO {
    @Pattern(regexp = "not_started|in_progress|paused|completed|settled|draft|submitted|pm_confirmed|confirmed|rejected|approved|paid|released|allocated|active|inactive", message = "状态值不合法")
    private String status;
    private String remark;
    @DecimalMin(value = "0.00", message = "确认金额不能小于 0")
    private BigDecimal confirmedAmount;
    private Long operatorId;
    private String operatorName;
}
