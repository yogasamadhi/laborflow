package com.company.labor.module.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "账号必填")
    private String username;
    @NotBlank(message = "密码必填")
    private String password;
    private String wechatOpenid;
    private String wechatNickname;
}
