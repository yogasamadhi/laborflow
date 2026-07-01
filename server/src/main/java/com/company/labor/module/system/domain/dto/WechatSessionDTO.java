package com.company.labor.module.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatSessionDTO {
    @NotBlank(message = "微信登录 code 必填")
    private String code;
}
