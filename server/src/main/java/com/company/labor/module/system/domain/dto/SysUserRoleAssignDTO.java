package com.company.labor.module.system.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public class SysUserRoleAssignDTO {
    private List<Long> roleIds;
}
