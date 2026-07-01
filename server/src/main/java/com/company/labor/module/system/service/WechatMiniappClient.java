package com.company.labor.module.system.service;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.config.WechatMiniappProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WechatMiniappClient {
    private final WechatMiniappProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WechatMiniappClient(WechatMiniappProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newHttpClient());
    }

    WechatMiniappClient(WechatMiniappProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public WechatCodeSession exchangeSession(String code) {
        if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getAppSecret())) {
            throw new BusinessException("微信小程序配置未启用");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(code2SessionUrl(code)))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("微信登录服务暂不可用");
            }
            WechatCodeSessionResponse body = objectMapper.readValue(response.body(), WechatCodeSessionResponse.class);
            if (body.getErrcode() != null && body.getErrcode() != 0) {
                throw new BusinessException("微信登录失败：" + safeMessage(body.getErrmsg()));
            }
            if (!StringUtils.hasText(body.getOpenid()) || !StringUtils.hasText(body.getSessionKey())) {
                throw new BusinessException("微信登录失败：未返回有效会话");
            }
            return new WechatCodeSession(body.getOpenid(), body.getSessionKey(), body.getUnionid());
        } catch (IOException ex) {
            throw new BusinessException("微信登录服务响应异常");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("微信登录服务请求中断");
        }
    }

    private String code2SessionUrl(String code) {
        return properties.getCode2SessionUrl()
                + "?appid=" + encode(properties.getAppId())
                + "&secret=" + encode(properties.getAppSecret())
                + "&js_code=" + encode(code)
                + "&grant_type=authorization_code";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String safeMessage(String message) {
        return StringUtils.hasText(message) ? message : "请稍后重试";
    }

    public record WechatCodeSession(String openid, String sessionKey, String unionid) {
    }

    @Data
    private static class WechatCodeSessionResponse {
        private String openid;
        @JsonProperty("session_key")
        private String sessionKey;
        private String unionid;
        private Integer errcode;
        private String errmsg;
    }
}
