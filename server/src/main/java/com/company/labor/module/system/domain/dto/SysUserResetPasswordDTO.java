package com.company.labor.module.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SysUserResetPasswordDTO {
    @NotBlank
    private String newPassword;
}
