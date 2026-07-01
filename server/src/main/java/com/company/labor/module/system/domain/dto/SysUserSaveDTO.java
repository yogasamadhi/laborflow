package com.company.labor.module.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Data;

@Data
public class SysUserSaveDTO {
    @NotBlank(message = "账号必填")
    private String username;
    private String password;
    private Long employeeId;
    private String nickname;
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;
    @Pattern(regexp = "active|inactive", message = "用户状态不合法")
    private String status;
    private List<Long> roleIds;
}
