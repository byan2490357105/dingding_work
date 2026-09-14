package com.example.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 第三方登录公共配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "third-party")
public class ThirdPartyProperties {

    /** 第三方回调出错时回跳的前端地址 */
    private String frontendErrorUrl = "http://localhost:5173/#/oauth/callback";
}
