package com.company.labor.module.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatSessionDTO {
    @NotBlank
    private String code;
}
