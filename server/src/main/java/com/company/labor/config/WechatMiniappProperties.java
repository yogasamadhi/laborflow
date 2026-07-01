package com.company.labor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "laborflow.wechat.miniapp")
public class WechatMiniappProperties {
    private String appId;
    private String appSecret;
    private String code2SessionUrl = "https://api.weixin.qq.com/sns/jscode2session";
}
