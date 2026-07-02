package com.company.labor.module.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "账号必填")
    @Size(min = 3, max = 64, message = "账号长度需为3-64位")
    private String username;

    @NotBlank(message = "密码必填")
    @Size(min = 8, max = 64, message = "密码长度需为8-64位")
    private String password;

    @NotBlank(message = "确认密码必填")
    private String confirmPassword;

    @Size(max = 64, message = "姓名/昵称不能超过64位")
    private String nickname;

    @Size(max = 32, message = "手机号不能超过32位")
    private String phone;
}
