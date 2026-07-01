package com.company.labor.module.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Data
public class SysUserSaveDTO {
    @NotBlank
    private String username;
    private String password;
    private Long employeeId;
    private String nickname;
    private String phone;
    private String status;
    private List<Long> roleIds;
}
