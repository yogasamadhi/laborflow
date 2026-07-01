package com.company.labor.module.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WechatSessionVO {
    private String openid;
    private String unionid;
    private boolean sessionEstablished;
    private boolean bound;
    private Long userId;
    private Long employeeId;
    private String nickname;
}
