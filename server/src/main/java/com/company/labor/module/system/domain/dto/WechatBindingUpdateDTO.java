package com.company.labor.module.system.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WechatBindingUpdateDTO {
    @NotNull
    private Long userId;
    private Long employeeId;
    private String nickname;
}
