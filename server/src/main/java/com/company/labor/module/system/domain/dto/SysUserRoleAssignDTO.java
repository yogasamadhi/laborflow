package com.company.labor.module.system.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class SysUserRoleAssignDTO {
    @NotEmpty(message = "至少选择一个角色")
    private List<Long> roleIds;
}
